package mardek.importer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.compressor.Bc1Compressor
import com.github.knokko.compressor.Bc1Worker
import mardek.assets.sprite.BcSprite
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
import java.awt.image.BufferedImage
import java.io.DataOutputStream
import java.io.OutputStream
import javax.imageio.ImageIO

class BcPacker {

	private val images = mutableListOf<BcSprite>()

	fun add(sprite: BcSprite) {
		images.add(sprite)
	}

	private fun compressBc1Images(boiler: BoilerInstance): MappedVkbBuffer {
		val compressor = Bc1Compressor(boiler)
		val worker = Bc1Worker(compressor)

		val bc1Images = images.filter { it.version == 1 }

		val totalSourceSize = 4L * bc1Images.sumOf { it.width * it.height }
		val sourceBuffer = boiler.buffers.createMapped(totalSourceSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "Bc1SourceBuffer")

		val destinationSize = totalSourceSize / 8
		val destinationBuffer = boiler.buffers.createMapped(destinationSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "Bc1DestinationBuffer")

		val pool = compressor.descriptorSetLayout.createPool(bc1Images.size, 0, "Bc1DescriptorPool")
		val descriptorSets = pool.allocate(bc1Images.size)

		val commands = SingleTimeCommands(boiler)
		commands.submit("Bc1Compression") { recorder ->
			var sourceOffset = 0L
			for ((index, sourceImage) in bc1Images.withIndex()) {
				val bufferedImage = sourceImage.bufferedImage as BufferedImage
				if (bufferedImage.width % 4 != 0 || bufferedImage.height % 4 != 0) {
					throw Error("Image dimensions must be multiples of 4 at this point")
				}
				val sourceSize = 4L * sourceImage.width * sourceImage.height
				boiler.buffers.encodeBufferedImageIntoRangeRGBA(sourceBuffer.mappedRange(sourceOffset, sourceSize), bufferedImage)
				val destinationOffset = sourceOffset / 8
				worker.compress(
					recorder, descriptorSets[index], sourceBuffer.range(sourceOffset, sourceSize),
					destinationBuffer.range(destinationOffset, sourceSize / 8), sourceImage.width, sourceImage.height
				)
				sourceOffset += sourceSize
			}
		}.awaitCompletion()
		commands.destroy()
		pool.destroy()
		worker.destroy()
		compressor.destroy(true)

		sourceBuffer.destroy(boiler)
		return destinationBuffer
	}

	fun compressImages(boiler: BoilerInstance) {
		images.sortBy { it.version }
		val destinationBuffer = compressBc1Images(boiler)

		var dataOffset = 0L
		for (image in images) {
			if (image.version != 1 || image.bufferedImage == null) continue
			val compressedDataSize = image.width * image.height / 2
			val compressedDataArray = ByteArray(compressedDataSize)
			destinationBuffer.mappedRange(dataOffset, compressedDataSize.toLong()).byteBuffer().get(compressedDataArray)
			image.data = compressedDataArray
			image.bufferedImage = null
			dataOffset += compressedDataSize
		}

		destinationBuffer.destroy(boiler)

		for (image in images) {
			if (image.bufferedImage == null) continue
			if (image.version == 7) throw UnsupportedOperationException("BC7 support isn't implemented yet")
		}
	}

	fun writeData(output: OutputStream) {
		val data = DataOutputStream(output)
		data.writeInt(images.size)

		for (image in images) {
			data.writeInt(image.width)
			data.writeInt(image.height)
			data.writeInt(image.version)
		}

		for ((index, image) in images.withIndex()) {
			image.index = index
			output.write(image.data!!)
			image.data = null
		}
	}
}