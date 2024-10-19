package mardek.renderer.area

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.HomogeneousDescriptorPool
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.pipelines.ShaderInfo
import mardek.assets.area.TileAnimationFrame
import mardek.renderer.StateRenderer
import mardek.state.area.AreaState
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memIntBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class AreaRenderer(
	private val state: AreaState,
	boiler: BoilerInstance,
	stack: MemoryStack,
	targetImageFormat: Int
): StateRenderer(boiler) {

	// TODO Reuse pipelineLayout, descriptorSet, imageSampler, descriptorSetLayout, and descriptorPool
	private val graphicsPipeline: Long
	private val pipelineLayout: Long
	private val descriptorSet: Long
	private val imageSampler: Long

	private val tileImages: VkbImage
	private val descriptorSetLayout: VkbDescriptorSetLayout
	private val descriptorPool: HomogeneousDescriptorPool
	private val mapBuffer: MappedVkbBuffer

	private val maxTileHeight = state.area.tileList.maxOf { tile -> tile.animations.maxOf { it.sprites.size } }
	private val spriteIndices = mutableMapOf<TileAnimationFrame, Int>()

	init {
		println("max tile height is $maxTileHeight")
		this.mapBuffer = boiler.buffers.createMapped(
			maxTileHeight * 4L * state.area.width * state.area.height,
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
			"MapBuffer${state.area.name}"
		)
		this.imageSampler = boiler.images.createSimpleSampler(
			VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
			VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "TileSampler"
		)

		val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(3, stack)
		boiler.descriptors.binding(descriptorBindings, 0,
			VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE,
			VK_SHADER_STAGE_FRAGMENT_BIT
		)
		boiler.descriptors.binding(descriptorBindings, 1,
			VK_DESCRIPTOR_TYPE_SAMPLER,
			VK_SHADER_STAGE_FRAGMENT_BIT
		)
		boiler.descriptors.binding(descriptorBindings, 2,
			VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
			VK_SHADER_STAGE_FRAGMENT_BIT
		)

		this.descriptorSetLayout = boiler.descriptors.createLayout(stack, descriptorBindings, "TilesDescriptorLayout")
		this.descriptorPool = descriptorSetLayout.createPool(1, 0, "TilesDescriptorPool")
		this.descriptorSet = descriptorPool.allocate(1)[0]

		val pushConstants = VkPushConstantRange.calloc(1, stack)
		pushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 20)

		this.pipelineLayout = boiler.pipelines.createLayout(
			pushConstants, "TilesPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
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
		val vertexModule: Long = boiler.pipelines.createShaderModule(
			"mardek/renderer/area/tiles.vert.spv", "TilesVertexShader"
		)
		val fragmentModule: Long = boiler.pipelines.createShaderModule(
			"mardek/renderer/area/tiles.frag.spv", "TilesFragmentShader"
		)
		builder.shaderStages(
			ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, vertexModule, null),
			ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, fragmentModule, specialization)
		)
		builder.noVertexInput()
		builder.simpleInputAssembly()
		builder.dynamicViewports(1)
		builder.simpleRasterization(VK_CULL_MODE_NONE)
		builder.noMultisampling()
		builder.noDepthStencil()
		builder.simpleColorBlending(1)
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
		builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, targetImageFormat)
		builder.ciPipeline.layout(this.pipelineLayout)
		this.graphicsPipeline = builder.build("TilesPipeline ${state.area.name}")
		vkDestroyShaderModule(boiler.vkDevice(), vertexModule, null)
		vkDestroyShaderModule(boiler.vkDevice(), fragmentModule, null)

		val numTileSprites = state.area.tileList.sumOf { tile -> tile.animations.sumOf { it.sprites.size }}
		this.tileImages = boiler.images.create(
			16, 16, VK_FORMAT_R8G8B8A8_SRGB,
			VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
			VK_IMAGE_ASPECT_COLOR_BIT, VK_SAMPLE_COUNT_1_BIT,
			1, numTileSprites, true, "TileImages ${state.area.name}"
		)

		val stagingBuffer = boiler.buffers.createMapped(
			4L * 16 * 16 * numTileSprites,
			VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
			"StagingBuffer${state.area.name}"
		)

		var imageIndex = 0
		for (tile in state.area.tileList) {
			for (frame in tile.animations) {
				spriteIndices[frame] = imageIndex
				for (image in frame.sprites) {
					boiler.buffers.encodeBufferedImageRGBA(stagingBuffer, image, 4L * 16 * 16 * imageIndex)
					imageIndex += 1
				}
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
						spriteIndices[animation]!! + animation.sprites.size - layer - 1
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
		barrier.image(tileImages.vkImage)
		barrier.subresourceRange().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, numTileSprites)
		vkCmdPipelineBarrier(
			recorder.commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
			0, null, null, imageBarriers
		)

		val copyRegions = VkBufferImageCopy.calloc(1, stack)
		val copy = copyRegions[0]
		copy.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, numTileSprites)
		copy.imageExtent().set(16, 16, 1)

		vkCmdCopyBufferToImage(
			recorder.commandBuffer,
			stagingBuffer.vkBuffer,
			tileImages.vkImage,
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
			VK_NULL_HANDLE, tileImages.vkImageView,
			VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
		)
		val samplerWrites = VkDescriptorImageInfo.calloc(1, stack)
		samplerWrites.get(0).set(imageSampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		val descriptorWrites = VkWriteDescriptorSet.calloc(3, stack)
		boiler.descriptors.writeImage(descriptorWrites, descriptorSet, 0,
			VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrites)
		boiler.descriptors.writeImage(descriptorWrites, descriptorSet, 1, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrites)
		boiler.descriptors.writeBuffer(stack, descriptorWrites, descriptorSet, 2,
			VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, mapBuffer.fullRange())
		vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
	}

	override fun render(recorder: CommandRecorder, targetImage: VkbImage) {
		// TODO Animate mapBuffer
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)
		recorder.bindGraphicsDescriptors(pipelineLayout, descriptorSet)
		for (layer in 0 until maxTileHeight) {
			vkCmdPushConstants(
				recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT,
				0, recorder.stack.ints(targetImage.width, targetImage.height, layer * state.area.width * state.area.height, 0, 0)
			)
			vkCmdDraw(recorder.commandBuffer, 6, 1, 0, 0)
		}
	}

	override fun destroy() {
		vkDestroySampler(boiler.vkDevice(), imageSampler, null)
		descriptorPool.destroy()
		descriptorSetLayout.destroy()
		tileImages.destroy(boiler)
		mapBuffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), this.graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), this.pipelineLayout, null)
	}
}
