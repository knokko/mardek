package mardek.renderer

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.text.TextInstance
import com.github.knokko.text.font.FontData
import com.github.knokko.text.font.UnicodeFonts
import com.github.knokko.ui.renderer.UiRenderInstance
import mardek.assets.area.StoredAreaRenderData
import mardek.renderer.area.*
import org.lwjgl.vulkan.VK10.*
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.util.*

class SharedResources(
	private val boiler: BoilerInstance, resourcePath: String, framesInFlight: Int, targetImageFormat: Int
) {

	val areaMap = mutableMapOf<UUID, MemoryAreaRenderData>()
	val kimRenderer: KimRenderer

	private val textInstance = TextInstance()
	val font = FontData(textInstance, UnicodeFonts.SOURCE)
	private val uiInstance = UiRenderInstance.withDynamicRendering(boiler, 0, targetImageFormat)
	val uiRenderers = (0 until framesInFlight).map { uiInstance.createRenderer() }

	val bc1Images: List<VkbImage>

	init {
		val startTime = System.nanoTime()

		val input = DataInputStream(BufferedInputStream(SharedResources::class.java.classLoader.getResourceAsStream(resourcePath)!!))

		val numImages = input.readInt()
		this.bc1Images = (0 until numImages).map {
			val width = input.readInt()
			val height = input.readInt()
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
				input.readFully(propagationBuffer, 0, stagingSize)
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

		this.kimRenderer = KimRenderer(
			boiler, targetImageFormat = targetImageFormat, framesInFlight = framesInFlight, spriteInput = input
		)

		@BitStruct(backwardCompatible = false)
		class StoredAreas(
			@BitField(ordering = 0)
			val list: ArrayList<StoredAreaRenderData>
		) {
			@Suppress("unused")
			constructor() : this(ArrayList(0))
		}

		// TODO Deserialize areas async because it slows down startup by ~200ms
		val storedAreas = Bitser(false).deserialize(StoredAreas::class.java, BitInputStream(input))
		for (stored in storedAreas.list) {
			areaMap[stored.areaID] = MemoryAreaRenderData.pack(stored)
		}

		input.close()

		commands.destroy()
		stagingBuffer.destroy(boiler)

		println("Preparing render resources took ${(System.nanoTime() - startTime) / 1_000_000} ms")
	}

	fun destroy() {
		kimRenderer.destroy()
		for (renderer in uiRenderers) renderer.destroy()
		for (image in bc1Images) image.destroy(boiler)
		uiInstance.destroy()
		font.destroy()
		textInstance.destroy()
	}
}
