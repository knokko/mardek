package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.buffers.SharedDeviceBufferBuilder
import com.github.knokko.boiler.buffers.SharedMappedBufferBuilder
import com.github.knokko.boiler.buffers.VkbBufferRange
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.pipelines.ShaderInfo
import com.github.knokko.boiler.synchronization.ResourceUsage
import mardek.assets.area.AreaCharacterModel
import mardek.assets.area.Tile
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

	private val tilePipeline: Long
	private val waterPipeline: Long

	private val images: VkbImage
	private val mapBuffer: VkbBufferRange
	private val waterBuffer: VkbBufferRange
	private val entityBuffer: MappedVkbBuffer

	private val tileSpriteIndices = mutableMapOf<Tile, Int>()
	private val entitySpriteIndices = mutableMapOf<AreaCharacterModel, Int>()

	private val entityDescriptorSet: Long
	private val tileDescriptorSet: Long
	private val waterDescriptorSet: Long

	private class ExtraEntity(val tileX: Int, val tileY: Int, val spriteIndex: Int)
	private val extraEntities = mutableListOf<ExtraEntity>()

	init {
		val storageAlignment = run {
			val props = VkPhysicalDeviceProperties.calloc(stack)
			vkGetPhysicalDeviceProperties(boiler.vkPhysicalDevice(), props)
			props.limits().minStorageBufferOffsetAlignment()
		}
		val baseMapBufferSize = 4L * state.area.width * state.area.height

		val bufferBuilder = SharedDeviceBufferBuilder(boiler)
		val mapBuffer = bufferBuilder.add(baseMapBufferSize, storageAlignment)
		val waterBuffer = bufferBuilder.add(baseMapBufferSize, storageAlignment)
		bufferBuilder.build(
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT,
			"MapBuffer ${state.area.name}"
		)
		this.mapBuffer = mapBuffer.get()
		this.waterBuffer = waterBuffer.get()

		val numTileSprites = state.area.tileList.sumOf { it.sprites.size }
		val numEntitySprites = story.getPlayableCharacters().sumOf { it.areaModel.allSprites.size }
		this.images = boiler.images.create(
			16, 16, VK_FORMAT_R8G8B8A8_SRGB,
			VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
			VK_IMAGE_ASPECT_COLOR_BIT, VK_SAMPLE_COUNT_1_BIT,
			1, numTileSprites + numEntitySprites + state.area.waterSprites.size,
			true, "AreaImages ${state.area.name}"
		)

		val spriteByteSize = 4L * 16 * 16

		val stagingBuilder = SharedMappedBufferBuilder(boiler)
		val getStagingSprites = stagingBuilder.add(
			(numTileSprites + numEntitySprites + state.area.waterSprites.size) * spriteByteSize, 4
		)
		val getStagingMapBuffer = stagingBuilder.add(this.mapBuffer.size, storageAlignment)
		val getStagingWaterBuffer = stagingBuilder.add(this.waterBuffer.size, storageAlignment)

		val stagingBuffer = stagingBuilder.build(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "StagingBuffer ${state.area.name}")
		val stagingSprites = getStagingSprites.get()
		val stagingMapBuffer = getStagingMapBuffer.get()
		val stagingWaterBuffer = getStagingWaterBuffer.get()

		var imageIndex = state.area.waterSprites.size
		for (tile in state.area.tileList) {
			tileSpriteIndices[tile] = imageIndex
			imageIndex += tile.sprites.size
		}

		for (y in 0 until state.area.height) {
			for (x in 0 until state.area.width) {
				val bufferIndex = x + y * state.area.width
				val tile = state.area.getTileAt(x, y)
				val baseSpriteIndex = tileSpriteIndices[tile]!!
				for (layer in 1 until tile.sprites.size) {
					val rightY = y - layer
					val extraIndex = tile.sprites.size - layer - 1
					extraEntities.add(ExtraEntity(x, rightY, baseSpriteIndex + extraIndex))
				}

				stagingMapBuffer.intBuffer().put(bufferIndex, baseSpriteIndex + tile.sprites.size - 1)
				stagingWaterBuffer.intBuffer().put(bufferIndex, state.area.getWaterTypeAt(x, y))
			}
		}

		val maxActiveEntities = 4 + extraEntities.size
		this.entityBuffer = boiler.buffers.createMapped(
			maxActiveEntities * 12L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, "AreaEntityBuffer ${state.area.name}"
		)

		val builder = GraphicsPipelineBuilder(boiler, stack)

		val specializationEntries = VkSpecializationMapEntry.calloc(2, stack)
		specializationEntries.get(0).set(0, 0, 4)
		specializationEntries.get(1).set(1, 4, 4)

		val specializationData = stack.calloc(8)
		specializationData.putInt(0, state.area.width)
		specializationData.putInt(4, state.area.height)

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
		this.tilePipeline = builder.build("TilesPipeline ${state.area.name}")

		builder.ciPipeline.pStages()!![1].module(resources.tiles.waterModule)
		this.waterPipeline = builder.build("WaterPipeline ${state.area.name}")

		for ((index, sprite) in state.area.waterSprites.withIndex()) {
			val stagingOffset = stagingSprites.offset + spriteByteSize * index
			boiler.buffers.encodeBufferedImageRGBA(stagingBuffer, sprite, stagingOffset)
		}
		for (tile in state.area.tileList) {
			for ((layer, sprite) in tile.sprites.withIndex()) {
				val stagingOffset = stagingSprites.offset + spriteByteSize * (tileSpriteIndices[tile]!! + layer)
				boiler.buffers.encodeBufferedImageRGBA(stagingBuffer, sprite, stagingOffset)
			}
		}
		for (character in story.getPlayableCharacters()) {
			entitySpriteIndices[character.areaModel] = imageIndex
			for (sprite in character.areaModel.allSprites) {
				val stagingOffset = stagingSprites.offset + spriteByteSize * imageIndex
				boiler.buffers.encodeBufferedImageRGBA(stagingBuffer, sprite, stagingOffset)
				imageIndex += 1
			}
		}

		resources.singleTimeCommands.submit("Staging ${state.area.name}") { recorder ->
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
				0, numTileSprites + numEntitySprites + state.area.waterSprites.size
			)
			vkCmdPipelineBarrier(
				recorder.commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
				0, null, null, imageBarriers
			)

			val copyRegions = VkBufferImageCopy.calloc(1, stack)
			val copy = copyRegions[0]
			copy.bufferOffset(stagingSprites.offset)
			copy.imageSubresource().set(
				VK_IMAGE_ASPECT_COLOR_BIT, 0, 0,
				numTileSprites + numEntitySprites + state.area.waterSprites.size
			)
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

			recorder.copyBufferRanges(stagingMapBuffer.range(), this.mapBuffer)
			recorder.copyBufferRanges(stagingWaterBuffer.range(), this.waterBuffer)
			recorder.bufferBarrier(this.mapBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
			))
			recorder.bufferBarrier(this.waterBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
			))
		}.awaitCompletion()
		stagingBuffer.destroy(boiler)

		val imageWrites = VkDescriptorImageInfo.calloc(1, stack)
		imageWrites.get(0).set(
			VK_NULL_HANDLE, images.vkImageView,
			VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
		)
		val samplerWrites = VkDescriptorImageInfo.calloc(1, stack)
		samplerWrites.get(0).set(resources.imageSampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		val waterSamplerWrites = VkDescriptorImageInfo.calloc(1, stack)
		waterSamplerWrites.get(0).set(resources.tiles.waterSampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

		this.tileDescriptorSet = resources.tiles.descriptorBank.borrowDescriptorSet("AreaTiles ${state.area.name}")
		this.waterDescriptorSet = resources.tiles.descriptorBank.borrowDescriptorSet("AreaWater ${state.area.name}")
		this.entityDescriptorSet = resources.entities.descriptorBank.borrowDescriptorSet("AreaEntities ${state.area.name}")

		val descriptorWrites = VkWriteDescriptorSet.calloc(8, stack)
		boiler.descriptors.writeImage(descriptorWrites, tileDescriptorSet, 0,
			VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrites)
		boiler.descriptors.writeImage(descriptorWrites, tileDescriptorSet, 1, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrites)
		boiler.descriptors.writeBuffer(stack, descriptorWrites, tileDescriptorSet, 2,
			VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, this.mapBuffer)

		boiler.descriptors.writeImage(
			descriptorWrites, entityDescriptorSet, 3, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrites
		)
		descriptorWrites.get(3).dstBinding(0)
		boiler.descriptors.writeImage(
			descriptorWrites, entityDescriptorSet, 4, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrites
		)
		descriptorWrites.get(4).dstBinding(1)

		boiler.descriptors.writeImage(
			descriptorWrites, waterDescriptorSet, 5, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrites
		)
		descriptorWrites.get(5).dstBinding(0)
		boiler.descriptors.writeImage(
			descriptorWrites, waterDescriptorSet, 6, VK_DESCRIPTOR_TYPE_SAMPLER, waterSamplerWrites
		)
		descriptorWrites.get(6).dstBinding(1)
		boiler.descriptors.writeBuffer(
			stack, descriptorWrites, waterDescriptorSet, 7, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, this.waterBuffer
		)
		descriptorWrites.get(7).dstBinding(2)
		vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
	}

	fun render(recorder: CommandRecorder, targetImage: VkbImage) {
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
			hostEntityBuffer.put(spriteIndex)
			numEntities += 1
		}

		val minCameraX = targetImage.width / 2 - scissorLeft
		val maxCameraX = state.area.width * tileSize - targetImage.width / 2 + scissorLeft
		if (state.area.width * tileSize > targetImage.width) cameraX = min(maxCameraX, max(minCameraX, cameraX))
		if (state.area.height * tileSize > targetImage.height) {
			cameraY = min(state.area.height * tileSize - targetImage.height / 2, max(targetImage.height / 2, cameraY))
		}

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, waterPipeline)
		recorder.bindGraphicsDescriptors(resources.tiles.pipelineLayout, waterDescriptorSet)
		vkCmdPushConstants(
			recorder.commandBuffer, resources.tiles.pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT, 0,
			recorder.stack.ints(targetImage.width, targetImage.height, cameraX, cameraY, scale)
		)
		vkCmdDraw(recorder.commandBuffer, 6, 1, 0, 0)

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, tilePipeline)
		recorder.bindGraphicsDescriptors(resources.tiles.pipelineLayout, tileDescriptorSet)
		vkCmdPushConstants(
			recorder.commandBuffer, resources.tiles.pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT, 0,
			recorder.stack.ints(targetImage.width, targetImage.height, cameraX, cameraY, scale)
		)
		vkCmdDraw(recorder.commandBuffer, 6, 1, 0, 0)

		for (entity in extraEntities) {
			hostEntityBuffer.put(tileSize * entity.tileX)
			hostEntityBuffer.put(tileSize * entity.tileY)
			hostEntityBuffer.put(entity.spriteIndex)
			numEntities += 1
		}

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, resources.entities.graphicsPipeline)
		vkCmdBindVertexBuffers(
			recorder.commandBuffer, 0,
			recorder.stack.longs(entityBuffer.vkBuffer),
			recorder.stack.longs(0)
		)
		recorder.bindGraphicsDescriptors(resources.entities.pipelineLayout, entityDescriptorSet)
		vkCmdPushConstants(
			recorder.commandBuffer, resources.entities.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0,
			recorder.stack.ints(targetImage.width, targetImage.height, cameraX, cameraY, scale)
		)
		vkCmdDraw(recorder.commandBuffer, 6, numEntities, 0, 0)
	}

	fun destroy() {
		images.destroy(boiler)
		mapBuffer.buffer.destroy(boiler)
		entityBuffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), this.tilePipeline, null)
		vkDestroyPipeline(boiler.vkDevice(), this.waterPipeline, null)
		resources.tiles.descriptorBank.returnDescriptorSet(tileDescriptorSet)
		resources.tiles.descriptorBank.returnDescriptorSet(waterDescriptorSet)
		resources.entities.descriptorBank.returnDescriptorSet(entityDescriptorSet)
	}
}
