package mardek.renderer

import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.PerFrameBuffer
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.text.TextInstance
import com.github.knokko.text.font.FontData
import com.github.knokko.text.font.UnicodeFonts
import com.github.knokko.ui.renderer.UiRenderInstance
import com.github.knokko.ui.renderer.UiRenderer
import mardek.renderer.area.*
import mardek.renderer.batch.*
import org.lwjgl.vulkan.VK10.*
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.*
import java.util.concurrent.CompletableFuture

class SharedResources(
	getBoiler: CompletableFuture<BoilerInstance>, framesInFlight: Int, targetImageFormat: CompletableFuture<Int>
) {

	private val boiler: BoilerInstance
	val areaMap = mutableMapOf<UUID, AreaRenderPair>()
	private lateinit var spriteManager: SpriteManager
	lateinit var kim1Renderer: Kim1Renderer
	lateinit var kim2Renderer: Kim2Renderer
	lateinit var colorGridRenderer: ColorGridRenderer
	val light: LightResources

	private val textInstance: TextInstance
	val font: FontData
	private val uiInstance: UiRenderInstance
	val uiRenderers: List<UiRenderer>

	val perFrameBuffer: PerFrameBuffer

	lateinit var bc1Images: List<VkbImage>

	init {
		val startTime = System.nanoTime()
		textInstance = TextInstance()
		font = FontData(textInstance, UnicodeFonts.SOURCE)
		println("Creating font took ${(System.nanoTime() - startTime) / 1000_000} ms")
		boiler = getBoiler.join()
		perFrameBuffer = PerFrameBuffer(boiler.buffers.createMapped(
			1000_000L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "PerFrameBuffer"
		).fullMappedRange())

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
				targetImageFormat = targetImageFormat.join(),
				framesInFlight = framesInFlight,
			)
			this.kim2Renderer = Kim2Renderer(
				boiler,
				perFrameBuffer = perFrameBuffer,
				spriteBuffer = spriteManager.spriteBuffer,
				targetImageFormat = targetImageFormat.join()
			)
			this.colorGridRenderer = ColorGridRenderer(boiler, perFrameBuffer, targetImageFormat.join())
		}
		kimThread.start()

		val bcThread = Thread {
			val bcInput = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(
				"mardek/game/bc1-sprites.bin"
			)!!))
			val numImages = bcInput.readInt()
			this.bc1Images = (0 until numImages).map {
				val width = bcInput.readInt()
				val height = bcInput.readInt()
				boiler.images.createSimple(
					width, height, VK_FORMAT_BC1_RGBA_SRGB_BLOCK,
					VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
					VK_IMAGE_ASPECT_COLOR_BIT, "Bc1Image$it"
				)
			}

			val totalSize = bc1Images.sumOf { it.width * it.height / 2L }
			val maxSize = bc1Images.maxOf { it.width * it.height / 2 }

			val stagingBuffer = boiler.buffers.createMapped(totalSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "UiStagingBuffer")

			val commands = SingleTimeCommands(boiler)
			commands.submit("UiStagingTransfer") { recorder ->
				for (image in bc1Images) {
					recorder.transitionLayout(image, null, ResourceUsage.TRANSFER_DEST)
				}

				val propagationBuffer = ByteArray(maxSize)
				var stagingOffset = 0L
				for (image in bc1Images) {
					val stagingSize = image.width * image.height / 2
					bcInput.readFully(propagationBuffer, 0, stagingSize)
					stagingBuffer.mappedRange(
						stagingOffset, stagingSize.toLong()).byteBuffer().put(propagationBuffer, 0, stagingSize
					)
					recorder.copyBufferToImage(image, stagingBuffer.range(stagingOffset, stagingSize.toLong()))
					stagingOffset += stagingSize
				}

				val destUsage = ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
				for (image in bc1Images) {
					recorder.transitionLayout(image, ResourceUsage.TRANSFER_DEST, destUsage)
				}
			}
			commands.destroy()
			stagingBuffer.destroy(boiler)
		}
		bcThread.start()

		uiInstance = UiRenderInstance.withDynamicRendering(boiler, 0, targetImageFormat.join())
		uiRenderers = (0 until framesInFlight).map { uiInstance.createRenderer(perFrameBuffer) }
		light = LightResources(boiler, targetImageFormat.join())
		kimThread.join()
		bcThread.join()
		areaThread.join()

		println("Preparing render resources took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy() {
		kim1Renderer.destroy()
		kim2Renderer.destroy()
		colorGridRenderer.destroy()
		for (renderer in uiRenderers) renderer.destroy()
		for (image in bc1Images) image.destroy(boiler)
		uiInstance.destroy()
		font.destroy()
		textInstance.destroy()
		perFrameBuffer.range.buffer.destroy(boiler)
		spriteManager.destroy(boiler)
		light.destroy(boiler)
	}
}
