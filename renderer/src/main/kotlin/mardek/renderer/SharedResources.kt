package mardek.renderer

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.buffers.SharedDeviceBufferBuilder
import com.github.knokko.boiler.buffers.SharedMappedBufferBuilder
import com.github.knokko.boiler.descriptors.SharedDescriptorPool
import com.github.knokko.boiler.descriptors.SharedDescriptorPoolBuilder
import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.memory.SharedMemoryAllocations
import com.github.knokko.boiler.memory.SharedMemoryBuilder
import com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple
import com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf
import com.github.knokko.text.TextInstance
import com.github.knokko.text.font.FontData
import com.github.knokko.text.font.UnicodeFonts
import com.github.knokko.ui.renderer.UiRenderInstance
import com.github.knokko.ui.renderer.UiRenderer
import mardek.renderer.area.*
import mardek.renderer.batch.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.*
import java.util.concurrent.CompletableFuture

class SharedResources(
	getBoiler: CompletableFuture<BoilerInstance>, framesInFlight: Int, skipWindow: Boolean = false
) {

	private val boiler: BoilerInstance
	val renderPass: Long
	val areaMap = mutableMapOf<UUID, AreaRenderPair>()
	private val spriteManager: SpriteManager
	private val descriptorPool: SharedDescriptorPool
	val kim1Renderer: Kim1Renderer
	val kim2Renderer: Kim2Renderer
	lateinit var colorGridRenderer: ColorGridRenderer
	lateinit var partRenderer: PartRenderer
	val light: LightResources

	private val textInstance: TextInstance
	val font: FontData
	private val uiInstance: UiRenderInstance
	val uiRenderers: List<UiRenderer>

	private val sharedAllocations: SharedMemoryAllocations
	val perFrameBuffer: PerFrameBuffer

	lateinit var bcImages: List<VkbImage>

	init {
		val startTime = System.nanoTime()
		textInstance = TextInstance()
		font = FontData(textInstance, UnicodeFonts.SOURCE)
		println("Creating font took ${(System.nanoTime() - startTime) / 1000_000} ms")
		boiler = getBoiler.join()

		renderPass = if (skipWindow) createRenderPass(boiler, VK_FORMAT_R8G8B8A8_SRGB) else createRenderPass(boiler)

		val sharedDescriptorBuilder = SharedDescriptorPoolBuilder(boiler)

		val sharedStorageBufferBuilder = SharedDeviceBufferBuilder(boiler)
		val sharedSpriteBufferBuilder = SharedDeviceBufferBuilder(boiler)
		val sharedStagingBufferBuilder = SharedMappedBufferBuilder(boiler)

		val kimInput = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(
			"mardek/game/kim-sprites.bin"
		)!!))

		val storageIntAlignment = stackPush().use { stack ->
			val deviceProperties = VkPhysicalDeviceProperties.calloc(stack)
			vkGetPhysicalDeviceProperties(boiler.vkPhysicalDevice(), deviceProperties)
			leastCommonMultiple(setOf(4L, deviceProperties.limits().minStorageBufferOffsetAlignment()))
		}

		spriteManager = SpriteManager(boiler, kimInput, sharedSpriteBufferBuilder, sharedStagingBufferBuilder, storageIntAlignment)
		kim1Renderer = Kim1Renderer(
			boiler,
			renderPass = renderPass,
			framesInFlight = framesInFlight,
			sharedDescriptorPoolBuilder = sharedDescriptorBuilder,
			sharedStorageBuilder = sharedStorageBufferBuilder,
			storageIntAlignment = storageIntAlignment
		)
		kim2Renderer = Kim2Renderer(
			boiler,
			renderPass = renderPass,
			sharedDescriptorPoolBuilder = sharedDescriptorBuilder,
		)

		val sharedMemoryAllocator = SharedMemoryBuilder(boiler)
		val bcImageLoader = BcImageLoader(boiler, sharedStagingBufferBuilder, sharedMemoryAllocator)
		sharedMemoryAllocator.add(sharedSpriteBufferBuilder.doNotBindMemory().build(
			VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "SpriteBuffer"
		))
		sharedMemoryAllocator.add(sharedStorageBufferBuilder.doNotBindMemory().build(
				VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "Kim1MiddleBuffer"
		))
		sharedMemoryAllocator.add(
			sharedStagingBufferBuilder,
			VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
			"SharedStagingBuffer"
		)

		uiInstance = UiRenderInstance.withRenderPass(boiler, sharedMemoryAllocator, renderPass, 0)
		sharedAllocations = sharedMemoryAllocator.allocate("SharedMemory", false)

		val areaThread = Thread {
			val areaInput = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(
				"mardek/game/area-offsets.bin"
			)!!))
			val bitser = Bitser(false)
			val numAreas = areaInput.readInt()
			for (counter in 0 until numAreas) {
				val id = UUID(areaInput.readLong(), areaInput.readLong())
				val storedLength = areaInput.readInt()
				val storedData = ByteArray(storedLength)
				areaInput.readFully(storedData)
				areaMap[id] = AreaRenderPair(bitser, storedData)
			}
			areaInput.close()
		}
		areaThread.start()

		val kimThread = Thread {
			spriteManager.initBuffers()
			kimInput.close()
			kim1Renderer.initBuffers()
			this.colorGridRenderer = ColorGridRenderer(boiler, renderPass, sharedDescriptorBuilder)
		}
		kimThread.start()

		bcImageLoader.fetchImages()
		val bcThread = Thread {
			bcImageLoader.transfer()
			this.bcImages = bcImageLoader.bcImages
			this.partRenderer = PartRenderer(boiler, bcImages, renderPass, sharedDescriptorBuilder)
		}
		bcThread.start()

		sharedDescriptorBuilder.request(uiInstance.baseDescriptorSetLayout, framesInFlight)
		sharedDescriptorBuilder.request(uiInstance.imageDescriptorSetLayout, bcImageLoader.bcImages.size)

		light = LightResources(boiler, renderPass)
		kimThread.join()
		bcThread.join()
		areaThread.join()
		descriptorPool = sharedDescriptorBuilder.build("SharedDescriptorPool")

		// Since the BC transfer is complete, its staging memory can be reused
		val mappedRange = bcImageLoader.getStagingBuffer.get()
		val perFrameOffset = nextMultipleOf(mappedRange.offset, storageIntAlignment)
		perFrameBuffer = PerFrameBuffer(mappedRange.buffer.mappedRange(perFrameOffset, 1_000_000L))
		uiRenderers = (0 until framesInFlight).map {
			val glyphRangeSize = 500_000L
			val glyphRange = mappedRange.buffer.mappedRange(nextMultipleOf(
				perFrameOffset + perFrameBuffer.range.size + it * glyphRangeSize, storageIntAlignment
			), glyphRangeSize)
			uiInstance.createRenderer(perFrameBuffer, glyphRange, descriptorPool)
		}

		kim1Renderer.initDescriptors(descriptorPool, spriteManager.spriteBuffer, perFrameBuffer)
		kim2Renderer.initDescriptors(descriptorPool, spriteManager.spriteBuffer, perFrameBuffer)
		colorGridRenderer.initDescriptors(descriptorPool, perFrameBuffer)
		partRenderer.initDescriptors(descriptorPool)

		println("Preparing render resources took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy() {
		descriptorPool.destroy(boiler)
		kim1Renderer.destroy()
		kim2Renderer.destroy()
		colorGridRenderer.destroy()
		partRenderer.destroy()
		for (renderer in uiRenderers) renderer.destroy()
		uiInstance.destroy()
		font.destroy()
		textInstance.destroy()
		light.destroy(boiler)
		vkDestroyRenderPass(boiler.vkDevice(), renderPass, null)
		sharedAllocations.free(boiler)
	}
}

fun createRenderPass(
	boiler: BoilerInstance, surfaceFormat: Int = boiler.window().surfaceFormat
) = stackPush().use { stack ->
	val attachments = VkAttachmentDescription.calloc(1, stack)
	val colorAttachment = attachments[0]
	colorAttachment.format(surfaceFormat)
	colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT)
	colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
	colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
	colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
	colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
	colorAttachment.initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
	colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

	val colorReference = VkAttachmentReference.calloc(1, stack)
	colorReference.attachment(0)
	colorReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

	val subpass = VkSubpassDescription.calloc(1, stack)
	subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
	subpass.pInputAttachments(null)
	subpass.colorAttachmentCount(1)
	subpass.pColorAttachments(colorReference)
	subpass.pResolveAttachments(null)
	subpass.pDepthStencilAttachment(null)
	subpass.pPreserveAttachments(null)

	val ciRenderPass = VkRenderPassCreateInfo.calloc(stack)
	ciRenderPass.`sType$Default`()
	ciRenderPass.pAttachments(attachments)
	ciRenderPass.pSubpasses(subpass)

	val pRenderPass = stack.callocLong(1)
	assertVkSuccess(
		vkCreateRenderPass(
			boiler.vkDevice(), ciRenderPass, null, pRenderPass
		), "CreateRenderPass", "MainRenderPass"
	)
	pRenderPass.get(0)
}