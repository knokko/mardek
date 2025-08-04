package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.placement.TextAlignment
import mardek.content.area.AreaDreamType
import mardek.content.area.Direction
import mardek.content.area.WaterType
import mardek.content.sprite.KimSprite
import mardek.renderer.RenderContext
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle
import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.vkCmdBindPipeline
import org.lwjgl.vulkan.VK10.vkCmdDraw
import org.lwjgl.vulkan.VK10.vkCmdPushConstants
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun renderCurrentArea(context: RenderContext, state: AreaState, region: Rectangle) {
	val kimBatch = context.addKim3Batch()

	val baseVisibleHorizontalTiles = region.width / 16.0
	val baseVisibleVerticalTiles = region.height / 16.0

	// The original MARDEK allow players to see at most 5 tiles above/below the player,
	// and at most 7 tiles left/right from the player.

	// I will aim for 6 tiles above/below the player, and let the aspect ratio determine the number of tiles
	// that can be seen left/right from the player, within reason.
	val floatScale = baseVisibleVerticalTiles / 13.0

	// Use integer scales to keep the tiles pretty
	val scale = max(1, floatScale.roundToInt())

	var cameraX = 0
	var cameraY = 0

	// Without restrictions, players with very wide screens/windows could see way too many tiles left/right
	// from the player. I will enforce a maximum of 14.5 tiles left/right, which is already ridiculous.
	val maxVisibleHorizontalTiles = 30.0
	val visibleHorizontalTiles = baseVisibleHorizontalTiles / scale

	// TODO Re-implement scissor
	var scissorLeft = 0
	if (visibleHorizontalTiles > maxVisibleHorizontalTiles) {
		scissorLeft = (region.width * ((visibleHorizontalTiles - maxVisibleHorizontalTiles) / visibleHorizontalTiles) / 2.0).roundToInt()
//		val scissors = VkRect2D.calloc(1, context.recorder.stack)
//		scissors.get(0).offset().set(scissorLeft, 0)
//		scissors.get(0).extent().set(context.viewportWidth - 2 * scissorLeft, context.viewportHeight)
//		vkCmdSetScissor(context.recorder.commandBuffer, 0, scissors)
	}

	val tileSize = 16 * scale
	val animationSize = 2

	val renderJobs = mutableListOf<EntityRenderJob>()

	for (chest in state.area.chests) {
		if (chest.hidden) continue
		val sprite = if (context.campaign.openedChests.contains(chest)) {
			chest.sprite.openedSprite
		} else chest.sprite.baseSprite

		renderJobs.add(EntityRenderJob(
			x = tileSize * chest.x,
			y = tileSize * chest.y,
			sprite = sprite,
		))
	}

	for (character in state.area.objects.characters) {
		val direction = character.startDirection ?: Direction.Down
		var spriteIndex = animationSize * direction.ordinal
		if (character.walkSpeed == -1) {
			if (state.currentTime.inWholeMilliseconds % 1000L >= 500L) spriteIndex += 1
		}
		renderJobs.add(EntityRenderJob(
			x = tileSize * character.startX,
			y = tileSize * character.startY - 4 * scale,
			sprite = character.sprites.sprites[spriteIndex]
		))
	}

	for (decoration in state.area.objects.decorations) {
		val spritesheet = decoration.sprites ?: continue
		val spriteIndex = (state.currentTime.inWholeMilliseconds % (decoration.timePerFrame * spritesheet.frames.size)) / decoration.timePerFrame

		val sprite = spritesheet.frames[spriteIndex.toInt()]
		renderJobs.add(EntityRenderJob(
			x = tileSize * decoration.x,
			y = tileSize * decoration.y,
			sprite = sprite
		))
	}

	for (door in state.area.objects.doors) {
		var spriteIndex = 0
		val openingDoor = state.openingDoor

		if (openingDoor != null && door == openingDoor.door) {
			val startTime = openingDoor.finishTime - AreaState.DOOR_OPEN_DURATION
			val progress = (state.currentTime - startTime) / AreaState.DOOR_OPEN_DURATION
			spriteIndex = min((door.sprites.frames.size * progress).toInt(), door.sprites.frames.size - 1)
		}
		renderJobs.add(EntityRenderJob(
			x = tileSize * door.x,
			y = tileSize * door.y,
			sprite = door.sprites.frames[spriteIndex]
		))
	}

	for (areaObject in state.area.objects.objects) {
		val spriteIndex = (state.currentTime.inWholeMilliseconds % (200L * areaObject.sprites.frames.size)) / 200L

		renderJobs.add(EntityRenderJob(
			x = tileSize * areaObject.x,
			y = tileSize * areaObject.y - 4 * scale,
			sprite = areaObject.sprites.frames[spriteIndex.toInt()]
		))
	}

	for (portal in state.area.objects.portals) {
		val spritesheet = portal.sprites ?: continue

		val isDream = state.area.properties.dreamType != AreaDreamType.None
		val destinationArea = portal.destination.area

		// When exactly 1 of the current area and destination is a dream area, the portal must be a dream circle
		// Hence we should not render the portal texture
		if (destinationArea != null && isDream != (destinationArea.properties.dreamType != AreaDreamType.None)) {
			continue
		}

		val spriteIndex = (state.currentTime.inWholeMilliseconds % (15000L * spritesheet.frames.size)) / 15000L
		renderJobs.add(EntityRenderJob(
			x = tileSize * portal.x,
			y = tileSize * portal.y,
			sprite = spritesheet.frames[spriteIndex.toInt()]
		))
	}

	for (gate in state.area.objects.switchGates) {
		renderJobs.add(EntityRenderJob(
			x = tileSize * gate.x,
			y = tileSize * gate.y,
			sprite = gate.color.gateSprite
		))
	}

	for (orb in state.area.objects.switchOrbs) {
		renderJobs.add(EntityRenderJob(
			x = tileSize * orb.x,
			y = tileSize * orb.y - 4 * scale,
			sprite = orb.color.onSprite
		))
	}

	for (platform in state.area.objects.switchPlatforms) {
		renderJobs.add(EntityRenderJob(
			x = tileSize * platform.x,
			y = tileSize * platform.y,
			sprite = platform.color.platformSprite
		))
	}

	for (transition in state.area.objects.transitions) {
		val arrow = transition.arrow ?: continue

		val period = 1000
		val relativeTime = state.currentTime.inWholeMilliseconds % period
		val opacity = 0.5 + 0.5 * sin(2 * PI * relativeTime / period)
		renderJobs.add(EntityRenderJob(
			x = tileSize * transition.x,
			y = tileSize * transition.y,
			sprite = arrow.sprite,
			opacity = opacity.toFloat()
		))
	}

	val nextPlayerPosition = state.nextPlayerPosition
	for ((index, character) in context.campaign.characterSelection.party.withIndex().reversed()) {
		if (character == null) continue
		val characterState = context.campaign.characterStates[character]!!

		var spriteIndex = 0
		val oldPosition = state.getPlayerPosition(index)

		var x = tileSize * oldPosition.x
		var y = tileSize * oldPosition.y

		if (nextPlayerPosition != null) {
			val newPosition = if (index > 0) state.getPlayerPosition(index - 1)
			else nextPlayerPosition.position

			val p = (state.currentTime - nextPlayerPosition.startTime) / (nextPlayerPosition.arrivalTime - nextPlayerPosition.startTime)
			x = (tileSize * ((1 - p) * oldPosition.x + p * newPosition.x)).roundToInt()
			y = (tileSize * ((1 - p) * oldPosition.y + p * newPosition.y)).roundToInt()

			if (p >= 0.25 && p < 0.75) spriteIndex += 1
		}

		if (index == 0) {
			cameraX = x + tileSize / 2
			cameraY = y + tileSize / 2
		}

		y -= 4 * scale

		val direction = if (index == 0) {
			if (nextPlayerPosition == null) state.lastPlayerDirection else Direction.delta(
				nextPlayerPosition.position.x - oldPosition.x,
				nextPlayerPosition.position.y - oldPosition.y
			)!!
		} else Direction.delta(
			state.getPlayerPosition(index - 1).x - oldPosition.x,
			state.getPlayerPosition(index - 1).y - oldPosition.y
		) ?: Direction.Down

		spriteIndex += animationSize * direction.ordinal

		val walkDamage = characterState.lastWalkDamage
		val walkDamageDuration = 500_000_000L
		val currentTime = System.nanoTime()
		val (blinkColor, blinkIntensity) = if (walkDamage != null && currentTime < walkDamage.time + walkDamageDuration) {
			val intensity = 1f - (currentTime - walkDamage.time).toFloat() / walkDamageDuration
			Pair(srgbToLinear(walkDamage.color), intensity)
		} else Pair(0, 0f)
		renderJobs.add(EntityRenderJob(
			x = x, y = y, sprite = character.areaSprites.sprites[spriteIndex],
			blinkColor = blinkColor, blinkIntensity = blinkIntensity
		))
	}

	//val renderData = context.resources.areaMap[state.area.id]!!.data
	val minTileX = max(0, (cameraX - region.width / 2) / tileSize)
	val minTileY = max(0, (cameraY - region.height / 2) / tileSize)
	val maxTileX = min(state.area.width - 1, 1 + (cameraX + region.width / 2) / tileSize)
	val maxTileY = min(state.area.height - 1, 1 + (cameraY + region.height / 2) / tileSize)
	for (tileX in minTileX .. maxTileX) {
		for (tileY in minTileY .. maxTileY) {
			if (tileY + 1 >= state.area.height) continue

			val renderX = tileX * tileSize
			val renderY = tileY * tileSize

			val midTile = state.area.getTile(tileX, tileY + 1)
			if (midTile.sprites.size > 1) {
				val sprite = midTile.sprites[midTile.sprites.size - 2]
				renderJobs.add(EntityRenderJob(
					x = renderX, y = renderY, sprite = sprite, sortY = renderY + tileSize / 2
				))
			}

			if (tileY + 2 >= state.area.height) continue
			val highTile = state.area.getTile(tileX, tileY + 2)
			if (highTile.sprites.size > 2) {
				val sprite = highTile.sprites[highTile.sprites.size - 3]
				renderJobs.add(EntityRenderJob(
					x = renderX, y = renderY, sprite = sprite, sortY = renderY + 3 * tileSize / 2
				))
			}
		}
	}

	renderJobs.sort()

	if (state.area.flags.noMovingCamera) {
		val minCameraX = region.width / 2 - scissorLeft
		val maxCameraX = state.area.width * tileSize - region.width / 2 + scissorLeft
		if (state.area.width * tileSize > region.width) cameraX = min(maxCameraX, max(minCameraX, cameraX))
		if (state.area.height * tileSize > region.height) {
			cameraY = min(
				state.area.height * tileSize - region.height / 2,
				max(region.height / 2, cameraY)
			)
		}
	}

	for (tileX in minTileX .. maxTileX) {
		for (tileY in minTileY .. maxTileY) {
			val renderX = region.minX + tileX * tileSize + region.width / 2 - cameraX
			val renderY = region.minY + tileY * tileSize + region.height / 2 -cameraY

			val waterType = state.area.getTile(tileX, tileY).waterType
			if (waterType != WaterType.None) {
				var backgroundSprite = state.area.tilesheet.waterSprites[0]
				val northWaterType = if (tileY == 0) WaterType.None
				else state.area.getTile(tileX, tileY - 1).waterType

				if (northWaterType == WaterType.None) {
					backgroundSprite = state.area.tilesheet.waterSprites[4]
				}

				val waterSprite = state.area.tilesheet.waterSprites[waterType.ordinal]
				kimBatch.simple(renderX, renderY, scale, backgroundSprite.offset)
				val opacity = if (waterType == WaterType.Water) 0.3f else 1f
				// TODO Respect opacity
				kimBatch.simple(renderX, renderY, scale, waterSprite.offset)
			}

			val sprite = state.area.getTile(tileX, tileY).sprites.last()
			kimBatch.simple(renderX, renderY, scale, sprite.offset)
		}
	}

	val incomingRandomBattle = state.incomingRandomBattle
	if (incomingRandomBattle != null) {
		val playerPosition = state.getPlayerPosition(0)
		renderJobs.add(EntityRenderJob(
			x = tileSize * playerPosition.x,
			y = tileSize * (playerPosition.y - 1) - 4 * scale,
			sprite = if (incomingRandomBattle.canAvoid) {
				context.content.ui.blueAlertBalloon
			} else context.content.ui.redAlertBalloon
		))
	}

	for (job in renderJobs) {
		var renderX = region.minX + job.x + region.width / 2 - cameraX
		var renderY = region.minY + job.y + region.height / 2 - cameraY
		if (job.sprite.width >= 32) renderX -= 16 * scale
		if (job.sprite.height >= 32) renderY -= 16 * scale
		val margin = 2 * tileSize
		if (renderX > -margin && renderY > -margin && renderX < context.frame.width + 2 * margin &&
			renderY < context.frame.height + 2 * margin
		) {
			kimBatch.simple(renderX, renderY, scale, job.sprite.offset)
			// TODO opacity & blink color & blink intensity: job.blinkColor, job.blinkIntensity, job.opacity
//			kimBatch.requests.add(KimRequest(
//				x = renderX, y = renderY, scale = scale.toFloat(), sprite = job.sprite, opacity = job.opacity,
//				blinkColor = job.blinkColor, blinkIntensity = job.blinkIntensity,
//			))
		}
	}

	val obtainedGold = state.obtainedGold
	if (obtainedGold != null) {
		val baseX = region.minX + tileSize * obtainedGold.chestX + context.frame.width / 2 - cameraX
		val baseY = region.minY + tileSize * obtainedGold.chestY + context.frame.height / 2 - cameraY
		kimBatch.simple(
			baseX - tileSize * 19 / 32,
			baseY - tileSize * 17 / 32,
			scale / 2f, context.content.ui.goldIcon.offset
		)
		// TODO Draw obtained gold
//		context.uiRenderer.beginBatch()
//		context.uiRenderer.drawString(
//			context.resources.font, "+${state.obtainedGold!!.amount}",
//			srgbToLinear(rgb(255, 204, 51)), intArrayOf(),
//			renderGold.baseX, 0, context.viewportWidth, context.viewportHeight,
//			renderGold.baseY - 2 * renderGold.scale, 6 * renderGold.scale, 1, TextAlignment.LEFT
//		)
//		context.uiRenderer.endBatch()
	}

	val obtainedItemStack = state.obtainedItemStack
	// TODO render loot
//	if (obtainedItemStack != null) {
//		this.lootRenderer = AreaLootRenderer(context, obtainedItemStack, scale)
//		this.lootRenderer!!.beforeRendering()
//	}
	val lightRequests = ArrayList<LightRequest>()
	val lightRadius = 24 * scale

	val minLightX = cameraX - context.frame.width / 2 - lightRadius
	val maxLightX = cameraX + context.frame.width / 2 + lightRadius
	val minLightY = cameraY - context.frame.height / 2 - lightRadius
	val maxLightY = cameraY + context.frame.height / 2 + lightRadius
	for (decoration in state.area.objects.decorations) {
		val light = decoration.light ?: continue
		val x = 16 * scale * decoration.x + 8 * scale
		val y = 16 * scale * decoration.y + scale * light.offsetY
		if (x in minLightX .. maxLightX && y in minLightY .. maxLightY) {
			lightRequests.add(LightRequest(x, y, srgbToLinear(light.color)))
		}
	}

	if (lightRequests.isNotEmpty()) {
//		vkCmdBindPipeline(
//			context.recorder.commandBuffer,
//			VK_PIPELINE_BIND_POINT_GRAPHICS,
//			context.resources.light.graphicsPipeline
//		)
//		vkCmdPushConstants(
//			context.recorder.commandBuffer, context.resources.light.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
//			0, context.recorder.stack.ints(context.viewportWidth, context.viewportHeight)
//		)
//		val vertexRange = context.resources.perFrameBuffer.allocate(LIGHT_VERTEX_SIZE.toLong() * lightRequests.size, 4L)
//		val vertexBuffer = vertexRange.intBuffer()
//		for (request in lightRequests) {
//			vertexBuffer.put(request.x + context.viewportWidth / 2 - cameraX - lightRadius)
//			vertexBuffer.put(request.y + context.viewportHeight / 2 - cameraY - lightRadius)
//			vertexBuffer.put(lightRadius)
//			vertexBuffer.put(request.color)
//		}
//		context.recorder.bindVertexBuffers(0, vertexRange)
//		vkCmdDraw(context.recorder.commandBuffer, 6, lightRequests.size, 0, 0)
	}
}

private class EntityRenderJob(
	val x: Int, val y: Int, val sprite: KimSprite, val opacity: Float = 1f, val sortY: Int = y,
	val blinkColor: Int = 0, val blinkIntensity: Float = 0f,
): Comparable<EntityRenderJob> {

	init {
		if (sprite.offset == -1) throw IllegalArgumentException()
	}

	override fun compareTo(other: EntityRenderJob) = this.sortY.compareTo(other.sortY)
}

private class LightRequest(val x: Int, val y: Int, val color: Int)
