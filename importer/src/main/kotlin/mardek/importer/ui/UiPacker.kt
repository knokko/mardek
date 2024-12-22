package mardek.importer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.compressor.Bc1Compressor
import com.github.knokko.compressor.Bc1Worker
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
import java.awt.image.BufferedImage
import java.io.DataOutputStream
import java.io.OutputStream
import javax.imageio.ImageIO

class UiPacker {

	private val bc1Images = mutableListOf<BufferedImage>()

	fun addBc1(path: String): Int {
		val input = UiPacker::class.java.getResourceAsStream(path) ?: throw IllegalArgumentException("Can't find $path")
		val image = ImageIO.read(input)
		input.close()

		if (image.width % 4 != 0 || image.height % 4 != 0) {
			throw IllegalArgumentException("Image dimensions ${image.width} x ${image.height} must be multiples of 4")
		}
		bc1Images.add(image)
		return bc1Images.size
	}

	fun writeDataAndDestroy(boiler: BoilerInstance, output: OutputStream) {
		val compressor = Bc1Compressor(boiler)
		val worker = Bc1Worker(compressor)

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
				val sourceSize = 4L * sourceImage.width * sourceImage.height
				boiler.buffers.encodeBufferedImageIntoRangeRGBA(sourceBuffer.mappedRange(sourceOffset, sourceSize), sourceImage)
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

		val data = DataOutputStream(output)
		data.writeInt(bc1Images.size)

		for (image in bc1Images) {
			data.writeInt(image.width)
			data.writeInt(image.height)
		}

		var dataOffset = 0L
		for (image in bc1Images) {
			val bc1DataArray = ByteArray(image.width * image.height / 2)
			destinationBuffer.mappedRange(dataOffset, bc1DataArray.size.toLong()).byteBuffer().get(bc1DataArray)
			data.write(bc1DataArray)
			dataOffset += bc1DataArray.size
		}

		destinationBuffer.destroy(boiler)
	}
}
