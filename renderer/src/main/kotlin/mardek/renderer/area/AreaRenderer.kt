package mardek.renderer.area

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.assets.area.Direction
import mardek.state.area.AreaState
import mardek.state.story.StoryState
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkRect2D
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AreaRenderer(
	private val state: AreaState,
	private val story: StoryState,
	private val resources: AreaResources,
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

		var numEntities = 0
		val hostEntityBuffer = resources.entityBuffers[frameIndex].intBuffer()
		val nextPlayerPosition = state.nextPlayerPosition
		for ((index, character) in story.party.withIndex().reversed()) {
			if (character == null) continue

			var spriteIndex = 0
			//var spriteIndex = entitySpriteIndices[character.areaModel] ?: throw IllegalStateException("Unexpected character $character")
			val oldPosition = state.getPlayerPosition(index)
			//val animationSize = character.areaModel.downSprites.size

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

			var directionX: Int
			var directionY: Int
			if (index == 0) {
				if (nextPlayerPosition == null) {
					directionX = state.lastPlayerDirectionX
					directionY = state.lastPlayerDirectionY
				} else {
					directionX = nextPlayerPosition.position.x - oldPosition.x
					directionY = nextPlayerPosition.position.y - oldPosition.y
				}
			} else {
				directionX = state.getPlayerPosition(index - 1).x - oldPosition.x
				directionY = state.getPlayerPosition(index - 1).y - oldPosition.y
			}

			if (directionY == -1) spriteIndex += animationSize
			if (directionX == 1) spriteIndex += 2 * animationSize
			if (directionX == -1) spriteIndex += 3 * animationSize

			hostEntityBuffer.put(x)
			hostEntityBuffer.put(y)
			hostEntityBuffer.put(character.areaSheet.indices!![spriteIndex])
			numEntities += 1
		}

		for (character in state.area.objects.characters) {
			hostEntityBuffer.put(tileSize * character.startX)
			hostEntityBuffer.put(tileSize * character.startY - 4 * scale)

			val direction = character.startDirection ?: Direction.Down
			val spriteIndex = animationSize * direction.ordinal
			hostEntityBuffer.put(character.spritesheet!!.indices!![spriteIndex])

			numEntities += 1
		}

		val minCameraX = targetImage.width / 2 - scissorLeft
		val maxCameraX = state.area.width * tileSize - targetImage.width / 2 + scissorLeft
		if (state.area.width * tileSize > targetImage.width) cameraX = min(maxCameraX, max(minCameraX, cameraX))
		if (state.area.height * tileSize > targetImage.height) {
			cameraY = min(state.area.height * tileSize - targetImage.height / 2, max(targetImage.height / 2, cameraY))
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
		vkCmdDraw(recorder.commandBuffer, 6, numEntities, 0, 0)

		renderTiles(resources.highTilesPipeline, state.area.renderHighTilesOffset)
	}
}
