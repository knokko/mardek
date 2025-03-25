package mardek.renderer

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.synchronization.ResourceUsage
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
import java.util.zip.InflaterInputStream
import kotlin.math.max

class SharedResources(
	getBoiler: CompletableFuture<BoilerInstance>, framesInFlight: Int, skipWindow: Boolean = false
) {

	private val boiler: BoilerInstance
	val renderPass: Long
	val areaMap = mutableMapOf<UUID, AreaRenderPair>()
	private lateinit var spriteManager: SpriteManager
	lateinit var kim1Renderer: Kim1Renderer
	lateinit var kim2Renderer: Kim2Renderer
	lateinit var colorGridRenderer: ColorGridRenderer
	lateinit var partRenderer: PartRenderer
	val light: LightResources

	private val textInstance: TextInstance
	val font: FontData
	private val uiInstance: UiRenderInstance
	val uiRenderers: List<UiRenderer>

	val perFrameBuffer: PerFrameBuffer

	lateinit var bcImages: List<VkbImage>

	init {
		val startTime = System.nanoTime()
		textInstance = TextInstance()
		font = FontData(textInstance, UnicodeFonts.SOURCE)
		println("Creating font took ${(System.nanoTime() - startTime) / 1000_000} ms")
		boiler = getBoiler.join()
		perFrameBuffer = PerFrameBuffer(boiler.buffers.createMapped(
			1000_000L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "PerFrameBuffer"
		).fullMappedRange())

		renderPass = if (skipWindow) createRenderPass(boiler, VK_FORMAT_R8G8B8A8_SRGB) else createRenderPass(boiler)

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
			val kimInput = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(
				"mardek/game/kim-sprites.bin"
			)!!))
			val spriteManager = SpriteManager(boiler, kimInput)
			kimInput.close()
			this.spriteManager = spriteManager
			this.kim1Renderer = Kim1Renderer(
				boiler,
				perFrameBuffer = perFrameBuffer,
				spriteBuffer = spriteManager.spriteBuffer,
				renderPass = renderPass,
				framesInFlight = framesInFlight,
			)
			this.kim2Renderer = Kim2Renderer(
				boiler,
				perFrameBuffer = perFrameBuffer,
				spriteBuffer = spriteManager.spriteBuffer,
				renderPass = renderPass
			)
			this.colorGridRenderer = ColorGridRenderer(boiler, renderPass, perFrameBuffer)
		}
		kimThread.start()

		val bcThread = Thread {
			val bcInput = DataInputStream(InflaterInputStream(SharedResources::class.java.classLoader.getResourceAsStream(
				"mardek/game/bc-sprites.bin"
			)!!))
			val numImages = bcInput.readInt()
			var totalSize = 0L
			var maxSize = 0
			val isBc1 = BooleanArray(numImages)
			val stagingOffsets = LongArray(numImages)

			fun computeByteSize(width: Int, height: Int, version: Int): Int {
				val baseSize = nextMultipleOf(width, 4) * nextMultipleOf(height, 4)
				return if (version == 1) baseSize / 2 else baseSize
			}

			this.bcImages = (0 until numImages).map {
				val width = bcInput.readInt()
				val height = bcInput.readInt()
				val version = bcInput.readInt()
				isBc1[it] = version == 1
				val byteSize = computeByteSize(width, height, version)

				// Ensure that offset is a multiple of texel block size
				totalSize = nextMultipleOf(totalSize, if (version == 1) 8 else 16)

				stagingOffsets[it] = totalSize
				val format = when (version) {
					1 -> VK_FORMAT_BC1_RGBA_SRGB_BLOCK
					7 -> VK_FORMAT_BC7_SRGB_BLOCK
					else -> throw UnsupportedOperationException("Unsupported BC version $version")
				}
				totalSize += byteSize
				maxSize = max(byteSize, maxSize)
				boiler.images.createSimple(
					width, height, format, VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
					VK_IMAGE_ASPECT_COLOR_BIT, "Bc${version}Image$it"
				)
			}
			this.partRenderer = PartRenderer(boiler, bcImages, renderPass)

			val stagingBuffer = boiler.buffers.createMapped(totalSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "BcStagingBuffer")

			val commands = SingleTimeCommands(boiler)
			commands.submit("BcStagingTransfer") { recorder ->
				recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, *bcImages.toTypedArray())

				val propagationBuffer = ByteArray(maxSize)
				val bufferCopyRegions = VkBufferImageCopy.calloc(1, recorder.stack)
				val copyRegion = bufferCopyRegions[0]
				copyRegion.bufferRowLength(0)
				copyRegion.bufferImageHeight(0)
				boiler.images.subresourceLayers(copyRegion.imageSubresource(), VK_IMAGE_ASPECT_COLOR_BIT)
				copyRegion.imageOffset()[0, 0] = 0
				for ((index, image) in bcImages.withIndex()) {
					val stagingSize = computeByteSize(image.width, image.height, if (isBc1[index]) 1 else 7)
					bcInput.readFully(propagationBuffer, 0, stagingSize)
					stagingBuffer.mappedRange(
						stagingOffsets[index], stagingSize.toLong()
					).byteBuffer().put(propagationBuffer, 0, stagingSize)

					copyRegion.bufferOffset(stagingOffsets[index])
					copyRegion.imageExtent()[image.width(), image.height()] = 1

					vkCmdCopyBufferToImage(
						recorder.commandBuffer,
						stagingBuffer.vkBuffer(),
						image.vkImage(),
						VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
						bufferCopyRegions
					)
				}

				val destUsage = ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
				recorder.bulkTransitionLayout(ResourceUsage.TRANSFER_DEST, destUsage, *bcImages.toTypedArray())
			}
			commands.destroy()
			stagingBuffer.destroy(boiler)
		}
		bcThread.start()

		uiInstance = UiRenderInstance.withRenderPass(boiler, renderPass, 0)
		uiRenderers = (0 until framesInFlight).map { uiInstance.createRenderer(perFrameBuffer) }
		light = LightResources(boiler, renderPass)
		kimThread.join()
		bcThread.join()
		areaThread.join()

		println("Preparing render resources took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy() {
		kim1Renderer.destroy()
		kim2Renderer.destroy()
		colorGridRenderer.destroy()
		partRenderer.destroy()
		for (renderer in uiRenderers) renderer.destroy()
		for (image in bcImages) image.destroy(boiler)
		uiInstance.destroy()
		font.destroy()
		textInstance.destroy()
		perFrameBuffer.range.buffer.destroy(boiler)
		spriteManager.destroy(boiler)
		light.destroy(boiler)
		vkDestroyRenderPass(boiler.vkDevice(), renderPass, null)
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