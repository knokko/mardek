package mardek.exporting

import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.zstd.ZSTDInBuffer
import org.lwjgl.util.zstd.ZSTDOutBuffer
import org.lwjgl.util.zstd.Zstd.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.Math.toIntExact
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class DecompressingInputStream(inputFile: File) : InputStream() {

	private val channel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ)
	private val zStream = ZSTD_createDStream()
	private val zInput = ZSTDInBuffer.calloc()
	private val zOutput = ZSTDOutBuffer.calloc()
	private val inputBuffer = memCalloc(BUFFER_SIZE)
	private val outputBuffer = memCalloc(BUFFER_SIZE)

	private var consumedOutputUntil = 0
	private var filledInputUntil = 0


	init {
		zInput.set(inputBuffer, 0)
		zOutput.set(outputBuffer, 0)
	}

	override fun read(): Int {
		val dummyArray = ByteArray(1)
		val readResult = read(dummyArray, 0, 1)
		return if (readResult == -1) -1 else dummyArray[0].toInt() and 0xFF
	}

	override fun read(destination: ByteArray, destinationOffset: Int, destinationAmount: Int): Int {
		if (destinationAmount == 0) return 0

		if (zOutput.pos() > 0L) {
			val numOutputBytes = toIntExact(zOutput.pos()) - consumedOutputUntil
			if (numOutputBytes <= destinationAmount) {
				outputBuffer.get(consumedOutputUntil, destination, destinationOffset, numOutputBytes)
				zOutput.pos(0L)
				consumedOutputUntil = 0
				return numOutputBytes
			} else {
				outputBuffer.get(consumedOutputUntil, destination, destinationOffset, destinationAmount)
				consumedOutputUntil += destinationAmount
				return destinationAmount
			}
		}

		if (filledInputUntil.toLong() == zInput.pos()) {
			inputBuffer.position(0)
			val numReadBytes = channel.read(inputBuffer)
			if (numReadBytes == -1) return -1
			filledInputUntil = numReadBytes
			zInput.pos(0)
		}

		val decompressResult = ZSTD_decompressStream(zStream, zOutput, zInput)
		if (ZSTD_isError(decompressResult)) {
			throw IOException("Decompression failed with $decompressResult (${ZSTD_getErrorName(decompressResult)}")
		}

		val numOutputBytes = toIntExact(zOutput.pos())
		if (numOutputBytes == 0) {
			if (decompressResult == 0L) return -1
			else throw IOException("Unexpected/weird end of stream")
		}
		if (numOutputBytes <= destinationAmount) {
			outputBuffer.get(0, destination, destinationOffset, numOutputBytes)
			zOutput.pos(0L)
			return numOutputBytes
		} else {
			outputBuffer.get(0, destination, destinationOffset, destinationAmount)
			consumedOutputUntil += destinationAmount
			return destinationAmount
		}
	}

	override fun close() {
		super.close()

		memFree(outputBuffer)
		memFree(inputBuffer)
		zOutput.free()
		zInput.free()
		ZSTD_freeDStream(zStream)
		channel.close()
	}

	companion object {

		private const val BUFFER_SIZE = 1024 * 1024
	}
}
