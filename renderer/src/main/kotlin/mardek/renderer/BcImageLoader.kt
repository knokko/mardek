package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBufferRange
import com.github.knokko.boiler.buffers.SharedMappedBufferBuilder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.images.ImageBuilder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.memory.SharedMemoryBuilder
import com.github.knokko.boiler.synchronization.ResourceUsage
import com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import java.io.DataInputStream
import java.util.function.Supplier
import java.util.zip.InflaterInputStream
import kotlin.math.max

class BcImageLoader(
	private val boiler: BoilerInstance,
	sharedStagingBufferBuilder: SharedMappedBufferBuilder,
	sharedMemoryBuilder: SharedMemoryBuilder,
) {

	private val bcInput = DataInputStream(InflaterInputStream(
		SharedResources::class.java.classLoader.getResourceAsStream("mardek/game/bc-sprites.bin")!!
	))

	private val numImages = bcInput.readInt()
	private val bcImageBuilders: List<Supplier<VkbImage>>
	lateinit var bcImages: List<VkbImage>
		private set
	private val isBc1 = BooleanArray(numImages)
	private val stagingOffsets = LongArray(numImages)
	val getStagingBuffer: Supplier<MappedVkbBufferRange>
	private val maxSize: Int

	private fun computeByteSize(width: Int, height: Int, version: Int): Int {
		val baseSize = nextMultipleOf(width, 4) * nextMultipleOf(height, 4)
		return if (version == 1) baseSize / 2 else baseSize
	}

	init {
		var totalSize = 0L
		var maxSize = 0

		this.bcImageBuilders = (0 until numImages).map {
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
			sharedMemoryBuilder.add(ImageBuilder("Bc${version}Image$it", width, height).texture().format(format))
		}

		this.getStagingBuffer = sharedStagingBufferBuilder.add(totalSize, 16) // texel block size is 16 bytes
		this.maxSize = maxSize
	}

	fun fetchImages() {
		this.bcImages = this.bcImageBuilders.map { it.get() }
	}

	fun transfer() {
		val stagingRange = getStagingBuffer.get()
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

				val childRange = stagingRange.childRange(stagingOffsets[index], stagingSize.toLong())
				childRange.byteBuffer().put(propagationBuffer, 0, stagingSize)

				copyRegion.bufferOffset(childRange.offset)
				copyRegion.imageExtent()[image.width(), image.height()] = 1

				vkCmdCopyBufferToImage(
					recorder.commandBuffer,
					childRange.buffer.vkBuffer,
					image.vkImage(),
					VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
					bufferCopyRegions
				)
			}

			val destUsage = ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
			recorder.bulkTransitionLayout(ResourceUsage.TRANSFER_DEST, destUsage, *bcImages.toTypedArray())
		}
		commands.destroy()
	}
}
