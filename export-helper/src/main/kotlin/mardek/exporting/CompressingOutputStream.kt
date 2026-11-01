package mardek.exporting

import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.zstd.ZSTDInBuffer
import org.lwjgl.util.zstd.ZSTDOutBuffer
import org.lwjgl.util.zstd.Zstd.*
import org.lwjgl.util.zstd.ZstdX
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.lang.Math.toIntExact
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.math.min

class CompressingOutputStream(outputFile: File, compressionLevel: Int) : OutputStream() {

	private val channel = FileChannel.open(
		outputFile.toPath(),
		StandardOpenOption.TRUNCATE_EXISTING,
		StandardOpenOption.CREATE,
		StandardOpenOption.WRITE,
	)
	private val zStream = ZSTD_createCCtx()
	private val zInput = ZSTDInBuffer.calloc()
	private val zOutput = ZSTDOutBuffer.calloc()
	private val inputBuffer = memCalloc(BUFFER_SIZE)
	private val outputBuffer = memCalloc(BUFFER_SIZE)

	init {
		ZSTD_CCtx_setParameter(zStream, ZSTD_c_compressionLevel, compressionLevel)
		zInput.set(inputBuffer, 0)
		zOutput.set(outputBuffer, 0)
	}

	override fun write(b: Int) {
		write(byteArrayOf(b.toByte()))
	}

	override fun write(source: ByteArray, sourceOffset: Int, sourceAmount: Int) {
		val sourceEndIndex = sourceOffset + sourceAmount
		var sourcePosition = sourceOffset
		while (sourcePosition < sourceEndIndex) {
			val batchSize = min(sourceEndIndex - sourcePosition, BUFFER_SIZE)
			inputBuffer.put(0, source, sourcePosition, batchSize)
			sourcePosition += batchSize

			ZSTDInBuffer.nsize(zInput.address(), batchSize.toLong())
			zInput.pos(0)

			while (zInput.pos() < batchSize) {
				zOutput.pos(0)
				// TODO BITSER Choose the endOp carefully!
				val compressionResult = ZSTD_compressStream2(zStream, zOutput, zInput, ZSTD_e_end)
				if (ZSTD_isError(compressionResult)) {
					throw IOException("Compression failed with $compressionResult (${ZSTD_getErrorName(compressionResult)})")
				}

				outputBuffer.position(0)
				outputBuffer.limit(toIntExact(zOutput.pos()))
				while (outputBuffer.position() < zOutput.pos()) {
					channel.write(outputBuffer)
				}
			}
		}
	}

	private fun flushOrClose(op: Int) {
		while (true) {
			zOutput.pos(0)
			zInput.pos(0)
			ZSTDInBuffer.nsize(zInput.address(), 0L)
			val compressionResult = ZSTD_compressStream2(zStream, zOutput, zInput, op)
			if (ZSTD_isError(compressionResult)) {
				throw IOException("Compression failed with $compressionResult (${ZSTD_getErrorName(compressionResult)})")
			}

			outputBuffer.position(0)
			outputBuffer.limit(toIntExact(zOutput.pos()))
			while (outputBuffer.position() < zOutput.pos()) {
				channel.write(outputBuffer)
			}
			outputBuffer.position(0)
			if (compressionResult == 0L) return
		}
	}

	override fun flush() {
		flushOrClose(ZSTD_e_flush)
		super.flush()
	}

	override fun close() {
		flushOrClose(ZSTD_e_end)
		super.close()

		channel.close()
		memFree(outputBuffer)
		memFree(inputBuffer)
		zOutput.free()
		zInput.free()
		ZSTD_freeCCtx(zStream)
	}

	companion object {
		private const val BUFFER_SIZE = 1024 * 1024
	}
}
