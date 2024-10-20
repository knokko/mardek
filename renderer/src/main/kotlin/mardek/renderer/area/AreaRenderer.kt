package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.pipelines.ShaderInfo
import mardek.assets.area.AreaCharacterModel
import mardek.assets.area.TileAnimationFrame
import mardek.state.area.AreaPosition
import mardek.state.area.AreaState
import mardek.state.story.StoryState
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memIntBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AreaRenderer(
	private val state: AreaState,
	private val story: StoryState,
	private val boiler: BoilerInstance,
	private val resources: SharedAreaResources,
	stack: MemoryStack
) {

	private val graphicsPipeline: Long

	private val images: VkbImage
	private val mapBuffer: MappedVkbBuffer

	private val maxTileHeight = state.area.tileList.maxOf { tile -> tile.animations.maxOf { it.sprites.size } }
	private val tileSpriteIndices = mutableMapOf<TileAnimationFrame, Int>()

	private val entityBuffer: MappedVkbBuffer
	private val entitySpriteIndices = mutableMapOf<AreaCharacterModel, Int>()

	init {
		println("max tile height is $maxTileHeight")
		this.mapBuffer = boiler.buffers.createMapped(
			maxTileHeight * 4L * state.area.width * state.area.height,
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
			"MapBuffer${state.area.name}"
		)
		val maxActiveEntities = 4 // Currently just the 4 party members
		this.entityBuffer = boiler.buffers.createMapped(
			maxActiveEntities * 12L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "AreaEntityBuffer ${state.area.name}"
		)

		val builder = GraphicsPipelineBuilder(boiler, stack)

		val specializationEntries = VkSpecializationMapEntry.calloc(3, stack)
		specializationEntries.get(0).set(0, 0, 4)
		specializationEntries.get(1).set(1, 4, 4)
		specializationEntries.get(2).set(2, 8, 4)

		val specializationData = stack.calloc(12)
		specializationData.putInt(0, state.area.width)
		specializationData.putInt(4, state.area.height)
		specializationData.putInt(8, maxTileHeight)

		val specialization = VkSpecializationInfo.calloc(stack)
		specialization.pMapEntries(specializationEntries)
		specialization.pData(specializationData)

		builder.shaderStages(
			ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, resources.tiles.vertexModule, null),
			ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, resources.tiles.fragmentModule, specialization)
		)
		builder.noVertexInput()
		builder.simpleInputAssembly()
		builder.dynamicViewports(1)
		builder.simpleRasterization(VK_CULL_MODE_NONE)
		builder.noMultisampling()
		builder.noDepthStencil()
		builder.simpleColorBlending(1)
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
		builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, resources.targetImageFormat)
		builder.ciPipeline.layout(resources.tiles.pipelineLayout)
		this.graphicsPipeline = builder.build("TilesPipeline ${state.area.name}")

		val numTileSprites = state.area.tileList.sumOf { tile -> tile.animations.sumOf { it.sprites.size }}
		val numEntitySprites = story.getPlayableCharacters().sumOf { it.areaModel.allSprites.size }
		this.images = boiler.images.create(
			16, 16, VK_FORMAT_R8G8B8A8_SRGB,
			VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
			VK_IMAGE_ASPECT_COLOR_BIT, VK_SAMPLE_COUNT_1_BIT,
			1, numTileSprites + numEntitySprites, true, "AreaImages ${state.area.name}"
		)

		val stagingBuffer = boiler.buffers.createMapped(
			4L * 16 * 16 * (numTileSprites + numEntitySprites),
			VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
			"StagingBuffer${state.area.name}"
		)

		var imageIndex = 0
		for (tile in state.area.tileList) {
			for (frame in tile.animations) {
				tileSpriteIndices[frame] = imageIndex
				for (image in frame.sprites) {
					boiler.buffers.encodeBufferedImageRGBA(stagingBuffer, image, 4L * 16 * 16 * imageIndex)
					imageIndex += 1
				}
			}
		}
		for (character in story.getPlayableCharacters()) {
			entitySpriteIndices[character.areaModel] = imageIndex
			for (sprite in character.areaModel.allSprites) {
				boiler.buffers.encodeBufferedImageRGBA(stagingBuffer, sprite, 4L * 16 * 16 * imageIndex)
				imageIndex += 1
			}
		}

		val mapIntBuffer = memIntBuffer(mapBuffer.hostAddress, state.area.width * state.area.height * maxTileHeight)
		for (index in 0 until mapIntBuffer.capacity()) mapIntBuffer.put(index, -1)
		for (y in 0 until state.area.height) {
			for (x in 0 until state.area.width) {
				val tile = state.area.getTileAt(x, y)
				val animation = tile.animations[0]
				// TODO Track tiles that require animation
				for (layer in 0 until animation.sprites.size) {
					val rightY = y - layer
					mapIntBuffer.put(
						x + rightY * state.area.width + layer * state.area.width * state.area.height,
						tileSpriteIndices[animation]!! + animation.sprites.size - layer - 1
					)
				}
			}
		}

		val stagingCommandPool = boiler.commands.createPool(0, boiler.queueFamilies().graphics.index, "StagingPool")
		val stagingCommandBuffer = boiler.commands.createPrimaryBuffers(stagingCommandPool, 1, "StagingCommands")[0]
		val recorder = CommandRecorder.begin(stagingCommandBuffer, boiler, stack, "StagingCommands")

		val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)
		val barrier = imageBarriers.get(0)
		barrier.`sType$Default`()
		barrier.srcAccessMask(0)
		barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
		barrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
		barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
		barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
		barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
		barrier.image(images.vkImage)
		barrier.subresourceRange().set(
			VK_IMAGE_ASPECT_COLOR_BIT, 0, 1,
			0, numTileSprites + numEntitySprites
		)
		vkCmdPipelineBarrier(
			recorder.commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
			0, null, null, imageBarriers
		)

		val copyRegions = VkBufferImageCopy.calloc(1, stack)
		val copy = copyRegions[0]
		copy.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, numTileSprites + numEntitySprites)
		copy.imageExtent().set(16, 16, 1)

		vkCmdCopyBufferToImage(
			recorder.commandBuffer,
			stagingBuffer.vkBuffer,
			images.vkImage,
			VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
			copyRegions
		)

		barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
		barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
		barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
		barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		vkCmdPipelineBarrier(
			recorder.commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
			0, null, null, imageBarriers
		)
		recorder.end()

		val fence = boiler.sync.fenceBank.borrowFence(false, "StagingFence")
		boiler.queueFamilies().graphics.first().submit(recorder.commandBuffer, "Staging", null, fence)
		fence.awaitSignal()
		boiler.sync.fenceBank.returnFence(fence)

		vkDestroyCommandPool(boiler.vkDevice(), stagingCommandPool, null)
		stagingBuffer.destroy(boiler)

		val imageWrites = VkDescriptorImageInfo.calloc(1, stack)
		imageWrites.get(0).set(
			VK_NULL_HANDLE, images.vkImageView,
			VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
		)
		val samplerWrites = VkDescriptorImageInfo.calloc(1, stack)
		samplerWrites.get(0).set(resources.imageSampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

		val descriptorWrites = VkWriteDescriptorSet.calloc(5, stack)
		boiler.descriptors.writeImage(descriptorWrites, resources.tiles.descriptorSet, 0,
			VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrites)
		boiler.descriptors.writeImage(descriptorWrites, resources.tiles.descriptorSet, 1, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrites)
		boiler.descriptors.writeBuffer(stack, descriptorWrites, resources.tiles.descriptorSet, 2,
			VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, mapBuffer.fullRange())

		boiler.descriptors.writeImage(
			descriptorWrites, resources.entities.descriptorSet, 3, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrites
		)
		descriptorWrites.get(3).dstBinding(0)
		boiler.descriptors.writeImage(
			descriptorWrites, resources.entities.descriptorSet, 4, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrites
		)
		descriptorWrites.get(4).dstBinding(1)
		vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
	}

	fun render(recorder: CommandRecorder, targetImage: VkbImage) {
		// TODO Animate mapBuffer

		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)

		// The original MARDEK allow players to see at most 5 tiles above/below the player,
		// and at most 7 tiles left/right from the player
		val baseHorizontalTiles = targetImage.width / 16.0
		val baseVerticalTiles = targetImage.height / 16.0
		val guessScaleX = baseHorizontalTiles / 15.0
		val guessScaleY = baseVerticalTiles / 11.0
		val guessScale = max(guessScaleX, guessScaleY)

		// Round the scale in favor of the player
		val scale = max(1, guessScale.toInt())
		val tileSize = 16 * scale

		var cameraX = 0
		var cameraY = 0

		var numEntities = 0
		val hostEntityBuffer = memIntBuffer(entityBuffer.hostAddress, entityBuffer.size.toInt() / 4)
		val nextPlayerPosition = state.nextPlayerPosition
		for ((index, character) in story.party.withIndex().reversed()) {
			if (character == null) continue

			var spriteIndex = entitySpriteIndices[character.areaModel] ?: throw IllegalStateException("Unexpected character $character")
			val oldPosition = state.getPlayerPosition(index)
			val animationSize = character.areaModel.downSprites.size

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

			y -= 4 * scale

			if (index == 0) {
				cameraX = x
				cameraY = y
			}

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
			hostEntityBuffer.put(spriteIndex)
			numEntities += 1
		}

		cameraX = min(state.area.width * tileSize - targetImage.width / 2, max(targetImage.width / 2, cameraX))
		cameraY = min(state.area.height * tileSize - targetImage.height / 2, max(targetImage.height / 2, cameraY))

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
		recorder.bindGraphicsDescriptors(resources.tiles.pipelineLayout, resources.tiles.descriptorSet)
		for (layer in 0 until maxTileHeight) {
			vkCmdPushConstants(
				recorder.commandBuffer, resources.tiles.pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT, 0,
				recorder.stack.ints(
					targetImage.width, targetImage.height, scale, layer * state.area.width * state.area.height,
					cameraX, cameraY
				)
			)
			vkCmdDraw(recorder.commandBuffer, 6, 1, 0, 0)
		}

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, resources.entities.graphicsPipeline)
		vkCmdBindVertexBuffers(
			recorder.commandBuffer, 0,
			recorder.stack.longs(entityBuffer.vkBuffer),
			recorder.stack.longs(0)
		)
		recorder.bindGraphicsDescriptors(resources.entities.pipelineLayout, resources.entities.descriptorSet)

		vkCmdPushConstants(
			recorder.commandBuffer, resources.entities.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0,
			recorder.stack.ints(targetImage.width, targetImage.height, cameraX, cameraY, scale)
		)
		vkCmdDraw(recorder.commandBuffer, 6, numEntities, 0, 0)
	}

	fun destroy() {
		images.destroy(boiler)
		mapBuffer.destroy(boiler)
		entityBuffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), this.graphicsPipeline, null)
	}
}
