package mardek.importer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.buffers.MappedVkbBuffer
import com.github.knokko.boiler.builders.BoilerBuilder
import com.github.knokko.boiler.commands.SingleTimeCommands
import com.github.knokko.boiler.descriptors.DescriptorCombiner
import com.github.knokko.boiler.memory.MemoryBlock
import com.github.knokko.boiler.memory.MemoryCombiner
import com.github.knokko.compressor.Bc1Compressor
import com.github.knokko.compressor.Bc1Worker
import com.github.knokko.compressor.Bc7Compressor.compressBc7
import mardek.content.sprite.BcSprite
import org.lwjgl.vulkan.VK10.*
import java.awt.image.BufferedImage
import java.io.DataOutputStream
import java.io.OutputStream
import java.lang.Math.toIntExact
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BcPacker {

	private val images = mutableListOf<BcSprite>()
	private val imageSet = mutableSetOf<BcSprite>()

	fun add(sprite: BcSprite) {
		if (imageSet.contains(sprite)) return
		images.add(sprite)
		imageSet.add(sprite)
	}

	private fun compressBc1Images(boiler: BoilerInstance): Pair<List<MappedVkbBuffer>, MemoryBlock> {
		val combiner = MemoryCombiner(boiler, "Bc1")
		val compressor = Bc1Compressor(boiler, combiner, combiner)
		val worker = Bc1Worker(compressor, 0, combiner)

		val bc1Images = images.filter { it.version == 1 }
		val sourceBuffers = bc1Images.map { combiner.addMappedBuffer(
			4L * it.width * it.height,
			boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
		) }

		val destinationBuffers = bc1Images.map { combiner.addMappedBuffer(
			it.width * it.height / 2L,
			boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
			VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
		) }
		val memory = combiner.build(false)

		val descriptorCombiner = DescriptorCombiner(boiler)
		val descriptorSets = descriptorCombiner.addMultiple(compressor.descriptorSetLayout, bc1Images.size)
		val descriptorPool = descriptorCombiner.build("Bc1DescriptorSets")

		SingleTimeCommands.submit(boiler, "Bc1Compression") { recorder ->
			compressor.performStagingTransfer(recorder)
			for ((index, sourceImage) in bc1Images.withIndex()) {
				val bufferedImage = sourceImage.bufferedImage as BufferedImage
				if (bufferedImage.width % 4 != 0 || bufferedImage.height % 4 != 0) {
					throw Error("Image dimensions must be multiples of 4 at this point")
				}
				sourceBuffers[index].encodeBufferedImage(bufferedImage)
				worker.compress(
					recorder, descriptorSets[index], sourceBuffers[index],
					destinationBuffers[index], sourceImage.width, sourceImage.height
				)
			}
		}.destroy()
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null)
		compressor.destroy()

		return Pair(destinationBuffers, memory)
	}

	fun compressImages() {
		images.sortBy { it.width }
		images.sortBy { it.height }
		images.sortBy { it.version }

		val threadPool = Executors.newFixedThreadPool(40)
		val submissions = mutableListOf<Future<*>>()
		submissions.add(threadPool.submit {
			val boiler = BoilerBuilder(
				VK_API_VERSION_1_0, "ExportBc1Sprites", 1
			).validation().forbidValidationErrors().build()
			val (destinationBuffers, memory) = compressBc1Images(boiler)

			var destinationIndex = 0
			for (image in images) {
				if (image.version != 1 || image.bufferedImage == null) continue
				val buffer = destinationBuffers[destinationIndex]
				val compressedDataArray = ByteArray(toIntExact(buffer.size))
				buffer.byteBuffer().get(compressedDataArray)
				image.data = compressedDataArray
				image.postEncodeCallback?.invoke()
				image.bufferedImage = null
				destinationIndex += 1
			}

			memory.destroy(boiler)
			boiler.destroyInitialObjects()
		})

		val counter = AtomicInteger()
		val total = images.filter { it.version == 7 && it.data == null }.size
		for (image in images) {
			if (image.version != 7 || image.data != null) continue
			submissions.add(threadPool.submit {
				image.data = compressBc7(image.bufferedImage as BufferedImage)
				image.postEncodeCallback?.invoke()
				image.bufferedImage = null

				val currentCounter = counter.incrementAndGet()
				if (currentCounter % 100 == 0) {
					println("compressed $currentCounter / $total bc7 images")
				}
			})
		}

		threadPool.shutdown()
		for (submission in submissions) submission.get()
		if (!threadPool.awaitTermination(200, TimeUnit.SECONDS)) {
			threadPool.shutdownNow()
			throw RuntimeException("Bc compression timed out")
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
