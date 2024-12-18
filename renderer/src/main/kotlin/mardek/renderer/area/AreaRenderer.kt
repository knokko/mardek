package mardek.renderer.area

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.assets.Campaign
import mardek.assets.area.AreaDreamType
import mardek.assets.area.Direction
import mardek.assets.area.WaterType
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharacterSelectionState
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkRect2D
import kotlin.math.*

class AreaRenderer(
		private val assets: Campaign,
		private val state: AreaState,
		private val characters: CharacterSelectionState,
		private val resources: SharedAreaResources,
) {

	fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		resources.kimRenderer.recordDuringRenderpass(recorder, targetImage, frameIndex)
	}

	fun beforeRendering(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {

		resources.kimRenderer.begin()

		val baseVisibleHorizontalTiles = targetImage.width / 16.0
		val baseVisibleVerticalTiles = targetImage.height / 16.0

		// The original MARDEK allow players to see at most 5 tiles above/below the player,
		// and at most 7 tiles left/right from the player.

		// I will aim for 6 tiles above/below the player, and let the aspect ratio determine the number of tiles
		// that can be seen left/right from the player, within reason.
		val floatScale = baseVisibleVerticalTiles / 13.0

		// Use integer scales to keep the tiles pretty
		val scale = max(1, floatScale.roundToInt())

		// Without restrictions, players with very wide screens/windows could see way too many tiles left/right
		// from the player. I will enforce a maximum of 14.5 tiles left/right, which is already ridiculous.
		val maxVisibleHorizontalTiles = 30.0
		val visibleHorizontalTiles = baseVisibleHorizontalTiles / scale

		var scissorLeft = 0
		if (visibleHorizontalTiles > maxVisibleHorizontalTiles) {
			scissorLeft = (targetImage.width * ((visibleHorizontalTiles - maxVisibleHorizontalTiles) / visibleHorizontalTiles) / 2.0).roundToInt()
			val scissors = VkRect2D.calloc(1, recorder.stack)
			scissors.get(0).offset().set(scissorLeft, 0)
			scissors.get(0).extent().set(targetImage.width - 2 * scissorLeft, targetImage.height)
			vkCmdSetScissor(recorder.commandBuffer, 0, scissors)
		}

		val tileSize = 16 * scale

		var cameraX = 0
		var cameraY = 0

		val animationSize = 2

		class EntityRenderJob(
				val x: Int, val y: Int, val sprite: Int, val opacity: Int = 255
		): Comparable<EntityRenderJob> {
			override fun compareTo(other: EntityRenderJob) = this.y.compareTo(other.y)
		}

		val renderJobs = mutableListOf<EntityRenderJob>()

		for (character in state.area.objects.characters) {
			val direction = character.startDirection ?: Direction.Down
			var spriteIndex = animationSize * direction.ordinal
			if (character.walkSpeed == -1) {
				if (state.currentTime.inWholeMilliseconds % 1000L >= 500L) spriteIndex += 1
			}
			renderJobs.add(EntityRenderJob(
					x = tileSize * character.startX,
					y = tileSize * character.startY - 4 * scale,
					sprite = character.sprites.sprites[spriteIndex].offset
			))
		}

		for (decoration in state.area.objects.decorations) {
			val spritesheet = decoration.sprites ?: continue
			val spriteIndex = (state.currentTime.inWholeMilliseconds % (decoration.timePerFrame * spritesheet.frames.size)) / decoration.timePerFrame

			renderJobs.add(EntityRenderJob(
					x = tileSize * decoration.x,
					y = tileSize * decoration.y,
					sprite = spritesheet.frames[spriteIndex.toInt()].offset
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
					sprite = door.sprites.frames[spriteIndex].offset
			))
		}

		for (areaObject in state.area.objects.objects) {
			val spriteIndex = (state.currentTime.inWholeMilliseconds % (200L * areaObject.sprites.frames.size)) / 200L

			renderJobs.add(EntityRenderJob(
					x = tileSize * areaObject.x,
					y = tileSize * areaObject.y - 4 * scale,
					sprite = areaObject.sprites.frames[spriteIndex.toInt()].offset
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

			val spriteIndex = (state.currentTime.inWholeMilliseconds % (500L * spritesheet.frames.size)) / 500L
			renderJobs.add(EntityRenderJob(
					x = tileSize * portal.x,
					y = tileSize * portal.y,
					sprite = spritesheet.frames[spriteIndex.toInt()].offset
			))
		}

		for (gate in state.area.objects.switchGates) {
			renderJobs.add(EntityRenderJob(
					x = tileSize * gate.x,
					y = tileSize * gate.y,
					sprite = gate.onSpriteOffset
			))
		}

		for (orb in state.area.objects.switchOrbs) {
			renderJobs.add(EntityRenderJob(
					x = tileSize * orb.x,
					y = tileSize * orb.y,
					sprite = orb.onSpriteOffset
			))
		}

		for (platform in state.area.objects.switchPlatforms) {
			renderJobs.add(EntityRenderJob(
					x = tileSize * platform.x,
					y = tileSize * platform.y,
					sprite = platform.onSpriteOffset
			))
		}

		for (transition in state.area.objects.transitions) {
			val spritesheet = transition.arrowSprite ?: continue

			val period = 1000
			val relativeTime = state.currentTime.inWholeMilliseconds % period
			val opacity = 0.5 + 0.5 * sin(2 * PI * relativeTime / period)
			renderJobs.add(EntityRenderJob(
					x = tileSize * transition.x,
					y = tileSize * transition.y,
					sprite = spritesheet.frames[0].offset,
					opacity = (255 * opacity).roundToInt()
			))
		}

		val nextPlayerPosition = state.nextPlayerPosition
		for ((index, character) in characters.party.withIndex().reversed()) {
			if (character == null) continue

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

			renderJobs.add(EntityRenderJob(
					x = x, y = y, sprite = character.areaSprites.sprites[spriteIndex].offset
			))
		}

		renderJobs.sort()

		if (state.area.flags.noMovingCamera) {
			val minCameraX = targetImage.width / 2 - scissorLeft
			val maxCameraX = state.area.width * tileSize - targetImage.width / 2 + scissorLeft
			if (state.area.width * tileSize > targetImage.width) cameraX = min(maxCameraX, max(minCameraX, cameraX))
			if (state.area.height * tileSize > targetImage.height) {
				cameraY = min(state.area.height * tileSize - targetImage.height / 2, max(targetImage.height / 2, cameraY))
			}
		}

		val minTileX = max(0, (cameraX - targetImage.width / 2) / tileSize)
		val minTileY = max(0, (cameraY - targetImage.height / 2) / tileSize)
		val maxTileX = min(state.area.width - 1, 1 + (cameraX + targetImage.width / 2) / tileSize)
		val maxTileY = min(state.area.height - 1, 1 + (cameraY + targetImage.height / 2) / tileSize)

		val renderData = resources.areaMap[state.area.id]!!
		for (tileX in minTileX .. maxTileX) {
			for (tileY in minTileY .. maxTileY) {
				val renderX = tileX * tileSize + targetImage.width / 2 - cameraX
				val renderY = tileY * tileSize + targetImage.height / 2 -cameraY

				val waterType = renderData.getWaterType(tileX, tileY)
				if (waterType != WaterType.None) {
					var backgroundOffset = renderData.waterSpriteOffsets[0]
					if (tileY > 0 && renderData.getWaterType(tileX, tileY - 1) == WaterType.None) {
						backgroundOffset = renderData.waterSpriteOffsets[4]
					}
					val waterOffset = renderData.waterSpriteOffsets[renderData.getWaterSpriteIndex(tileX, tileY)]
					resources.kimRenderer.render(KimRequest(x = renderX, y = renderY, scale = scale, spriteOffset = backgroundOffset, opacity = 1f))
					resources.kimRenderer.render(KimRequest(x = renderX, y = renderY, scale = scale, spriteOffset = waterOffset, opacity = 0.3f))
				}
				val spriteOffset = renderData.tileSpriteOffsets[renderData.getTileSpriteIndex(tileX, tileY, 0)]
				resources.kimRenderer.render(KimRequest(x = renderX, y = renderY, scale = scale, spriteOffset = spriteOffset, opacity = 1f))
			}
		}

		for (job in renderJobs) {
			val renderX = job.x + targetImage.width / 2 - cameraX
			val renderY = job.y + targetImage.height / 2 - cameraY
			val margin = 2 * tileSize
			if (renderX > -margin && renderY > -margin && renderX < targetImage.width + 2 * margin && renderY < targetImage.height + 2 * margin) {
//				resources.kimRenderer.render(KimRequest(
//					x = renderX, y = renderY, scale = scale, spriteOffset = job.sprite, opacity = job.opacity / 255f
//				))
			}
		}

		for (tileX in minTileX .. maxTileX) {
			for (tileY in minTileY .. maxTileY) {
				val renderX = tileX * tileSize + targetImage.width / 2 - cameraX
				val renderY = tileY * tileSize + targetImage.height / 2 -cameraY

				val midIndex = renderData.getTileSpriteIndex(tileX, tileY, 1)
				if (midIndex != 1023) {
					val spriteOffset = renderData.tileSpriteOffsets[midIndex]
					resources.kimRenderer.render(KimRequest(x = renderX, y = renderY, scale = scale, spriteOffset = spriteOffset, opacity = 1f))
				}
				val highIndex = renderData.getTileSpriteIndex(tileX, tileY, 2)
				if (highIndex != 1023) {
					val spriteOffset = renderData.tileSpriteOffsets[highIndex]
					resources.kimRenderer.render(KimRequest(x = renderX, y = renderY, scale = scale, spriteOffset = spriteOffset, opacity = 1f))
				}
			}
		}

		resources.kimRenderer.recordBeforeRenderpass(recorder, targetImage, frameIndex)
	}
}
