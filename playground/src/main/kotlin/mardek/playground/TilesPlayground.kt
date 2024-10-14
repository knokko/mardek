package mardek.playground

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.builders.WindowBuilder
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.descriptors.HomogeneousDescriptorPool
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.window.AcquiredImage
import com.github.knokko.boiler.window.SimpleWindowRenderLoop
import com.github.knokko.boiler.window.VkbWindow
import com.github.knokko.boiler.window.WindowEventLoop
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memIntBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR
import org.lwjgl.vulkan.VK12.*
import javax.imageio.ImageIO

fun main() {
	val boiler = BoilerBuilder(
		VK_API_VERSION_1_2, "TilesPlayground", 1
	)
		.validation()
		.enableDynamicRendering()
		.addWindow(WindowBuilder(1600, 900, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
		.build()

	val eventLoop = WindowEventLoop()
	eventLoop.addWindow(TilesPlayground(boiler.window()))
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}

class TilesPlayground(window: VkbWindow) : SimpleWindowRenderLoop(
	window, 2, false, VK_PRESENT_MODE_MAILBOX_KHR,
	ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
) {

	private var graphicsPipeline = 0L
	private var pipelineLayout = 0L
	private var descriptorSet = 0L
	private var imageSampler = 0L

	private lateinit var tileImages: VkbImage
	private lateinit var descriptorSetLayout: VkbDescriptorSetLayout
	private lateinit var descriptorPool: HomogeneousDescriptorPool
	private lateinit var mapBuffer: MappedVkbBuffer

	private val mapWidth = Tiles.map[0].size
	private val mapHeight = Tiles.map.size

	override fun setup(boiler: BoilerInstance, stack: MemoryStack) {
		super.setup(boiler, stack)

		this.mapBuffer = boiler.buffers.createMapped(
			4L * mapWidth * mapHeight, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "MapBuffer"
		)

		this.imageSampler = boiler.images.createSampler(
			VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER,
			0f, 0f, true, "TileSampler"
		)

		val descriptorBindings = VkDescriptorSetLayoutBinding.calloc(3, stack)
		boiler.descriptors.binding(descriptorBindings, 0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, VK_SHADER_STAGE_FRAGMENT_BIT)
		boiler.descriptors.binding(descriptorBindings, 1, VK_DESCRIPTOR_TYPE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT)
		boiler.descriptors.binding(descriptorBindings, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT)

		this.descriptorSetLayout = boiler.descriptors.createLayout(stack, descriptorBindings, "TilesDescriptorLayout")
		this.descriptorPool = descriptorSetLayout.createPool(1, 0, "TilesDescriptorPool")
		this.descriptorSet = descriptorPool.allocate(1)[0]

		val pushConstants = VkPushConstantRange.calloc(1, stack)
		pushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 16)

		this.pipelineLayout = boiler.pipelines.createLayout(
			pushConstants, "TilesPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
		)

		val builder = GraphicsPipelineBuilder(boiler, stack)
		builder.simpleShaderStages(
			"Tiles", "mardek/playground/shaders/tiles.vert.spv",
			"mardek/playground/shaders/tiles.frag.spv"
		)
		builder.noVertexInput()
		builder.simpleInputAssembly()
		builder.dynamicViewports(1)
		builder.simpleRasterization(VK_CULL_MODE_NONE)
		builder.noMultisampling()
		builder.noDepthStencil()
		builder.simpleColorBlending(1)
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
		builder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, window.surfaceFormat)
		builder.ciPipeline.layout(this.pipelineLayout)
		this.graphicsPipeline = builder.build("TilesPipeline")
		
		val tileset = "aeropolis"

		val tileIdMapping = mutableMapOf<Int, Int>()
		val mapIntBuffer = memIntBuffer(mapBuffer.hostAddress(), mapWidth * mapHeight)
		for (row in Tiles.map) {
			for (tileId in row) {
				if (!tileIdMapping.containsKey(tileId)) tileIdMapping[tileId] = tileIdMapping.size
			}
		}

		for (y in 0 until mapHeight) {
			for (x in 0 until mapWidth) {
				val mapIndex = x + y * mapWidth
				mapIntBuffer.put(mapIndex, tileIdMapping[Tiles.map[y][x]]!!)
			}
		}

		this.tileImages = boiler.images.create(
			16, 16, VK_FORMAT_R8G8B8A8_SRGB,
			VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
			VK_IMAGE_ASPECT_COLOR_BIT, VK_SAMPLE_COUNT_1_BIT,
			1, tileIdMapping.size, true, "TileImages"
		)

		val stagingBuffer = boiler.buffers.createMapped(
			4L * 16 * 16 * tileIdMapping.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "StagingBuffer"
		)
		val tileSheet = ImageIO.read(TilesPlayground::class.java.getResourceAsStream("tilesets/aeropolis.png"))
		for ((tileId, tileIndex) in tileIdMapping) {
//			val rawOffsetX = tileId % 40 - 10
//			val rawTileHeight = 1 + rawOffsetX / 10
			if (tileId < 10) throw UnsupportedOperationException()
			val (rawOffsetX, rawOffsetY) = if (tileId < 100) Pair((tileId - 10) % 30, 1)
			else if (tileId < 1100) Pair(((tileId - 110) % 10), (tileId - 110) / 10 + 2)
			else Pair((tileId - 1100) % 10, (tileId - 1100) / 10 + 11)
			//val tileHeight = 16 * (1 + rawTileHeight)
			println("size is ${stagingBuffer.size} and tileIndex is $tileIndex and tileId is $tileId so raw offset is ($rawOffsetX, $rawOffsetY)")
			boiler.buffers.encodeBufferedImageRGBA(
				stagingBuffer, tileSheet.getSubimage(16 * rawOffsetX, 16 * rawOffsetY, 16, 16), 4L * 16 * 16 * tileIndex
			)
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
		barrier.subresourceRange().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, tileIdMapping.size)
		vkCmdPipelineBarrier(
			recorder.commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
			0, null, null, imageBarriers
		)

		val copyRegions = VkBufferImageCopy.calloc(1, stack)
		val copy = copyRegions[0]
		copy.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, tileIdMapping.size)
		copy.imageExtent().set(16, 16, 1)

		vkCmdCopyBufferToImage(
			recorder.commandBuffer, stagingBuffer.vkBuffer, tileImages.vkImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
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
		imageWrites.get(0).set(VK_NULL_HANDLE, tileImages.vkImageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		val samplerWrites = VkDescriptorImageInfo.calloc(1, stack)
		samplerWrites.get(0).set(imageSampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
		val descriptorWrites = VkWriteDescriptorSet.calloc(3, stack)
		boiler.descriptors.writeImage(descriptorWrites, descriptorSet, 0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrites)
		boiler.descriptors.writeImage(descriptorWrites, descriptorSet, 1, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrites)
		boiler.descriptors.writeBuffer(stack, descriptorWrites, descriptorSet, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, mapBuffer.fullRange())
		vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null)
	}

	private var lastTime = System.nanoTime()
	private var lastCounter = 0

	override fun recordFrame(
		stack: MemoryStack,
		frameIndex: Int,
		recorder: CommandRecorder,
		acquiredImage: AcquiredImage,
		instance: BoilerInstance
	) {
		val currentTime = System.nanoTime()
		if (currentTime - lastTime > 1_000_000_000L) {
			println("fps is $lastCounter")
			lastTime = currentTime
			lastCounter = 0
		}
		lastCounter += 1
		val colorAttachments = VkRenderingAttachmentInfo.calloc(1, stack)
		recorder.simpleColorRenderingAttachment(
			colorAttachments[0], acquiredImage.image().vkImageView, VK_ATTACHMENT_LOAD_OP_CLEAR,
			VK_ATTACHMENT_STORE_OP_STORE, 0.2f, 0.4f, 0.8f, 1f
		)
		recorder.beginSimpleDynamicRendering(
			acquiredImage.width(), acquiredImage.height(),
			colorAttachments, null, null
		)
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
		recorder.bindGraphicsDescriptors(pipelineLayout, descriptorSet)
		vkCmdPushConstants(
			recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_FRAGMENT_BIT,
			0, stack.ints(acquiredImage.width(), acquiredImage.height(), mapWidth, mapHeight)
		)
		recorder.dynamicViewportAndScissor(acquiredImage.width(), acquiredImage.height())
		vkCmdDraw(recorder.commandBuffer, 6, 1, 0, 0)
		recorder.endDynamicRendering()
	}

	override fun cleanUp(boiler: BoilerInstance) {
		super.cleanUp(boiler)
		vkDestroySampler(boiler.vkDevice(), imageSampler, null)
		descriptorPool.destroy()
		descriptorSetLayout.destroy()
		tileImages.destroy(boiler)
		mapBuffer.destroy(boiler)
		vkDestroyPipeline(boiler.vkDevice(), this.graphicsPipeline, null)
		vkDestroyPipelineLayout(boiler.vkDevice(), this.pipelineLayout, null)
	}
}
