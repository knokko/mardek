package mardek.renderer.area

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.assets.GameAssets
import mardek.assets.area.AreaDreamType
import mardek.assets.area.Direction
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharactersState
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkRect2D
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AreaRenderer(
	private val assets: GameAssets,
	private val state: AreaState,
	private val characters: CharactersState,
	private val resources: SharedAreaResources,
) {

	fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)

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

		val animationSize = 2 // TODO Maybe stop hardcoding this

		val hostEntityBuffer = resources.entityBuffers[frameIndex].intBuffer()

		for (character in state.area.objects.characters) {
			hostEntityBuffer.put(tileSize * character.startX)
			hostEntityBuffer.put(tileSize * character.startY - 4 * scale)

			val direction = character.startDirection ?: Direction.Down
			val spriteIndex = animationSize * direction.ordinal
			hostEntityBuffer.put(character.spritesheet!!.indices!![spriteIndex])
		}

		for (decoration in state.area.objects.decorations) {
			val spritesheet = decoration.spritesheet ?: continue

			hostEntityBuffer.put(tileSize * decoration.x)
			hostEntityBuffer.put(tileSize * decoration.y)
			hostEntityBuffer.put(spritesheet.indices!![0])
		}

		for (door in state.area.objects.doors) {
			hostEntityBuffer.put(tileSize * door.x)
			hostEntityBuffer.put(tileSize * door.y)
			hostEntityBuffer.put(door.spritesheet!!.indices!![0])
		}

		for (areaObject in state.area.objects.objects) {
			val spritesheet = areaObject.spritesheet ?: continue

			hostEntityBuffer.put(tileSize * areaObject.x)
			hostEntityBuffer.put(tileSize * areaObject.y - 4 * scale)
			hostEntityBuffer.put(spritesheet.indices!![0])
		}

		for (portal in state.area.objects.portals) {
			val spritesheet = portal.spritesheet ?: continue

			val isDream = state.area.properties.dreamType != AreaDreamType.None
			val destination = assets.areas.find { it.properties.rawName == portal.destination.areaName } ?: continue

			// When exactly 1 of the current area and destination is a dream area, the portal must be a dream circle
			// Hence we should not render the portal texture
			if (isDream != (destination.properties.dreamType != AreaDreamType.None)) continue

			hostEntityBuffer.put(tileSize * portal.x)
			hostEntityBuffer.put(tileSize * portal.y)
			hostEntityBuffer.put(spritesheet.indices!![0])
		}

		for (gate in state.area.objects.switchGates) {
			hostEntityBuffer.put(tileSize * gate.x)
			hostEntityBuffer.put(tileSize * gate.y)
			hostEntityBuffer.put(gate.onSpriteOffset)
		}

		for (orb in state.area.objects.switchOrbs) {
			hostEntityBuffer.put(tileSize * orb.x)
			hostEntityBuffer.put(tileSize * orb.y)
			hostEntityBuffer.put(orb.onSpriteOffset)
		}

		for (platform in state.area.objects.switchPlatforms) {
			hostEntityBuffer.put(tileSize * platform.x)
			hostEntityBuffer.put(tileSize * platform.y)
			hostEntityBuffer.put(platform.onSpriteOffset)
		}

		for (transition in state.area.objects.transitions) {
			val spritesheet = transition.arrowSprite ?: continue

			hostEntityBuffer.put(tileSize * transition.x)
			hostEntityBuffer.put(tileSize * transition.y)
			hostEntityBuffer.put(spritesheet.indices!![0])
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

			hostEntityBuffer.put(x)
			hostEntityBuffer.put(y)
			hostEntityBuffer.put(character.areaSheet.indices!![spriteIndex])
		}

		if (state.area.flags.noMovingCamera) {
			val minCameraX = targetImage.width / 2 - scissorLeft
			val maxCameraX = state.area.width * tileSize - targetImage.width / 2 + scissorLeft
			if (state.area.width * tileSize > targetImage.width) cameraX = min(maxCameraX, max(minCameraX, cameraX))
			if (state.area.height * tileSize > targetImage.height) {
				cameraY = min(state.area.height * tileSize - targetImage.height / 2, max(targetImage.height / 2, cameraY))
			}
		}

		fun renderTiles(pipeline: Long, mapOffset: Int) {
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
			recorder.bindGraphicsDescriptors(resources.tilesPipelineLayout, resources.descriptorSet)

			val water = state.area.waterSpriteOffsets
			vkCmdPushConstants(recorder.commandBuffer, resources.tilesPipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT, 0, recorder.stack.ints(
				state.area.width, state.area.height,
				targetImage.width, targetImage.height,
				cameraX, cameraY, scale, mapOffset,
				water[0], water[1], water[2], water[3], water[4]
			))
			vkCmdDraw(recorder.commandBuffer, 6, 1, 0, 0)
		}

		renderTiles(resources.lowTilesPipeline, state.area.renderLowTilesOffset)

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, resources.entitiesPipeline)
		vkCmdBindVertexBuffers(
			recorder.commandBuffer, 0,
			recorder.stack.longs(resources.entityBuffers[frameIndex].buffer.vkBuffer),
			recorder.stack.longs(resources.entityBuffers[frameIndex].offset)
		)
		recorder.bindGraphicsDescriptors(resources.entitiesPipelineLayout, resources.descriptorSet)
		vkCmdPushConstants(
			recorder.commandBuffer, resources.entitiesPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0,
			recorder.stack.ints(targetImage.width, targetImage.height, cameraX, cameraY, scale)
		)
		vkCmdDraw(recorder.commandBuffer, 6, hostEntityBuffer.position() / 3, 0, 0)

		renderTiles(resources.highTilesPipeline, state.area.renderHighTilesOffset)
	}
}
