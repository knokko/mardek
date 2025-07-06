package mardek.renderer

import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf
import org.lwjgl.vulkan.VK10.*
import java.io.DataInputStream
import java.util.zip.InflaterInputStream
import kotlin.math.max

class BcImageLoader(
	stagingCombiner: MemoryCombiner,
	persistentCombiner: MemoryCombiner,
) {

	private val bcInput = DataInputStream(InflaterInputStream(
		SharedResources::class.java.classLoader.getResourceAsStream("mardek/game/bc-sprites.bin")!!
	))

	val bcImages: List<VkbImage>
	private val stagingBuffers: MutableList<MappedVkbBuffer>
	private val maxSize: Int

	private fun computeByteSize(width: Int, height: Int, version: Int): Int {
		val baseSize = nextMultipleOf(width, 4) * nextMultipleOf(height, 4)
		return if (version == 1) baseSize / 2 else baseSize
	}

	init {
		var maxSize = 0

		val numImages = bcInput.readInt()
		val imagesAndBuffers = (0 until numImages).map {
			val width = bcInput.readInt()
			val height = bcInput.readInt()
			val version = bcInput.readInt()
			val byteSize = computeByteSize(width, height, version)

			val format = when (version) {
				1 -> VK_FORMAT_BC1_RGBA_SRGB_BLOCK
				7 -> VK_FORMAT_BC7_SRGB_BLOCK
				else -> throw UnsupportedOperationException("Unsupported BC version $version")
			}
			maxSize = max(byteSize, maxSize)
			val image = persistentCombiner.addImage(ImageBuilder(
				"Bc${version}Image$it", width, height).texture().format(format), 0.5f
			)
			val buffer = stagingCombiner.addMappedBuffer(
				byteSize.toLong(),
				if (version == 1) 8 else 16,
				VK_BUFFER_USAGE_TRANSFER_SRC_BIT
			)
			Pair(image, buffer)
		}

		this.maxSize = maxSize
		this.bcImages = imagesAndBuffers.map { it.first }
		this.stagingBuffers = imagesAndBuffers.map { it.second }.toMutableList()
	}

	fun prepare() {
		val propagationBuffer = ByteArray(maxSize)
		for (buffer in stagingBuffers) {
			bcInput.readFully(propagationBuffer, 0, buffer.size.toInt())
			buffer.byteBuffer().put(propagationBuffer, 0, buffer.size.toInt())
		}
	}

	fun prepare(recorder: CommandRecorder) {
		recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, *bcImages.toTypedArray())
		recorder.bulkCopyBufferToImage(bcImages.toTypedArray(), stagingBuffers.toTypedArray())
		val destinationUsage = ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
		recorder.bulkTransitionLayout(
			ResourceUsage.TRANSFER_DEST, destinationUsage, *bcImages.toTypedArray()
		)
		stagingBuffers.clear()
	}
}
