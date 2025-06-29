package mardek.renderer

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.memory.MemoryBlock
import com.github.knokko.boiler.memory.MemoryCombiner
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
	private val descriptorPool: Long
	val kim1Renderer: Kim1Renderer
	val kim2Renderer: Kim2Renderer
	val colorGridRenderer: ColorGridRenderer
	val partRenderer: PartRenderer
	val rectangleRenderer: RectangleRenderer
	val light: LightResources

	private val textInstance: TextInstance
	val font: FontData
	private val uiInstance: UiRenderInstance
	val uiRenderers: List<UiRenderer>

	private val memory: MemoryBlock
	val perFrameBuffer: PerFrameBuffer

	lateinit var bcImages: List<VkbImage>

	init {
		val startTime = System.nanoTime()
		textInstance = TextInstance()
		font = FontData(textInstance, UnicodeFonts.SOURCE)
		println("Creating font took ${(System.nanoTime() - startTime) / 1000_000} ms")
		boiler = getBoiler.join()

		val areaThread = Thread {
			val areaInput = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(
				"mardek/game/area-offsets.bin"
			)!!))
			val bitser = Bitser(false)
			val numAreas = areaInput.readInt()
			repeat(numAreas) {
				val id = UUID(areaInput.readLong(), areaInput.readLong())
				val storedLength = areaInput.readInt()
				val storedData = ByteArray(storedLength)
				areaInput.readFully(storedData)
				areaMap[id] = AreaRenderPair(bitser, storedData)
			}
			areaInput.close()
		}
		areaThread.start()

		val stagingCombiner = MemoryCombiner(boiler, "StagingMemory")
		val persistentCombiner = MemoryCombiner(boiler, "PersistentMemory")
		val descriptorCombiner = DescriptorCombiner(boiler)

		val bcImageLoader = BcImageLoader(stagingCombiner, persistentCombiner)
		renderPass = if (skipWindow) createRenderPass(boiler, VK_FORMAT_R8G8B8A8_SRGB) else createRenderPass(boiler)

		val kimInput = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(
			"mardek/game/kim-sprites.bin"
		)!!))

		perFrameBuffer = PerFrameBuffer(persistentCombiner.addMappedBuffer(
			1000_000L, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
		))
		spriteManager = SpriteManager(boiler, kimInput, persistentCombiner, stagingCombiner)
		kim1Renderer = Kim1Renderer(
			boiler,
			perFrameBuffer = perFrameBuffer,
			renderPass = renderPass,
			framesInFlight = framesInFlight,
			descriptorCombiner = descriptorCombiner,
			persistentCombiner = persistentCombiner,
		)
		kim2Renderer = Kim2Renderer(
			boiler,
			perFrameBuffer = perFrameBuffer,
			renderPass = renderPass,
			descriptorCombiner = descriptorCombiner
		)
		colorGridRenderer = ColorGridRenderer(boiler, perFrameBuffer, renderPass, descriptorCombiner)
		rectangleRenderer = RectangleRenderer(boiler, perFrameBuffer, renderPass)

		uiInstance = UiRenderInstance.withRenderPass(
			boiler, renderPass, 0, persistentCombiner,
			descriptorCombiner, bcImageLoader.bcImages.size
		)
		uiRenderers = (0 until framesInFlight).map { uiInstance.createRenderer(
			perFrameBuffer, 500_000, persistentCombiner, descriptorCombiner
		)}

		memory = persistentCombiner.build(false)
		val stagingMemory = stagingCombiner.build(false)

		val bcThread = Thread {
			bcImageLoader.prepare()
			this.bcImages = bcImageLoader.bcImages
		}
		bcThread.start()

		val kimThread = Thread {
			spriteManager.prepare()
			kimInput.close()
		}
		kimThread.start()

		light = LightResources(boiler, renderPass)
		kimThread.join()
		bcThread.join()
		areaThread.join()

		partRenderer = PartRenderer(boiler, bcImages, renderPass, descriptorCombiner)
		SingleTimeCommands.submit(boiler, "Preparation") { recorder ->
			spriteManager.prepare(recorder)
			bcImageLoader.prepare(recorder)
			uiInstance.prepare(recorder)
		}.destroy()

		stagingMemory.destroy(boiler)

		descriptorPool = descriptorCombiner.build("SharedDescriptorPool")

		kim1Renderer.prepare(spriteManager.spriteBuffer)
		kim2Renderer.prepare(spriteManager.spriteBuffer)
		colorGridRenderer.prepare()
		partRenderer.prepare()
		for (uiRenderer in uiRenderers) uiRenderer.prepare()

		println("Preparing render resources took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy() {
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null)
		rectangleRenderer.destroy()
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
		memory.destroy(boiler)
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
