
package mardek.exporting

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.lwjgl.BufferUtils
import org.lwjgl.util.zstd.ZSTDInBuffer
import org.lwjgl.util.zstd.ZSTDOutBuffer
import org.lwjgl.util.zstd.Zstd
import org.lwjgl.util.zstd.Zstd.ZSTD_CCtx_setParameter
import org.lwjgl.util.zstd.Zstd.ZSTD_c_compressionLevel
import java.io.File
import java.lang.Math.toIntExact
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.random.Random

class TestCompressingOutputStream {

	@Suppress("KotlinMisorderedAssertEqualsArguments")
	private fun runTest(payload: ByteArray) {
		val testFile = Files.createTempFile("", "")
		testFile.toFile().deleteOnExit()

		val output = CompressingOutputStream(testFile.toFile(), 5)
		output.write(payload)
		output.flush()
		output.close()

		val compressed = Files.readAllBytes(testFile)
		testFile.deleteIfExists()

		val test = ContentWrapper.compress(payload, 5)
		assertArrayEquals(payload, ContentWrapper.decompress(test))
		assertArrayEquals(payload, ContentWrapper.decompress(compressed))
	}

	@Test
	fun testEmptyFile() {
		runTest(ByteArray(0))
	}

	@Test
	fun testSingleByteFile() {
		runTest(byteArrayOf(12))
	}

	@Test
	fun testSmallFile() {
		runTest(ByteArray(100) { it.toByte() })
	}

	@Test
	fun testLargerFile() {
		runTest(ByteArray(50 * 999 * 888) { it.toByte() })
	}

	@Test
	fun testMediumRandomFile() {
		runTest(Random.nextBytes(64000))
	}

	@Test
	fun testLargerRandomFile() {
		runTest(Random.nextBytes(50 * 999 * 888))
	}

	@Test
	fun testMySanity() {
		val zStream = Zstd.ZSTD_createCStream()
		ZSTD_CCtx_setParameter(zStream, ZSTD_c_compressionLevel, 5)
		val emptyInput = ZSTDInBuffer.create()
		emptyInput.set(BufferUtils.createByteBuffer(0), 0)

		val outputBuffer = BufferUtils.createByteBuffer(9)
		val zOutput = ZSTDOutBuffer.create()
		zOutput.set(outputBuffer, 0)
		assertEquals(0, Zstd.ZSTD_compressStream2(zStream, zOutput, emptyInput, Zstd.ZSTD_e_end))

		val compressedArray = ByteArray(outputBuffer.capacity()) { outputBuffer.get(it) }
		println("compressed array is ${compressedArray.contentToString()}")
		println("simple compressed array is ${ContentWrapper.compress(ByteArray(0), 5).contentToString()}")

		val testFile = File("test")
		val compressionStream = CompressingOutputStream(testFile, 5)
		compressionStream.write(ByteArray(0))
		//compressionStream.flush()
		compressionStream.close()
		println("from file is ${Files.readAllBytes(testFile.toPath()).contentToString()}")
		assertEquals(0, ContentWrapper.decompress(Files.readAllBytes(testFile.toPath())).size)
	}

	@Test
	fun testMySanity2() {
		val zStream = Zstd.ZSTD_createCStream()
		ZSTD_CCtx_setParameter(zStream, ZSTD_c_compressionLevel, 5)

		val inputBuffer = BufferUtils.createByteBuffer(1)
		inputBuffer.put(0, 12)
		val zInput = ZSTDInBuffer.create()
		zInput.set(inputBuffer, 0)

		val outputBuffer = BufferUtils.createByteBuffer(20)
		val zOutput = ZSTDOutBuffer.create()
		zOutput.set(outputBuffer, 0)
		assertEquals(0, Zstd.ZSTD_compressStream2(zStream, zOutput, zInput, Zstd.ZSTD_e_end))

		val compressedArray = ByteArray(toIntExact(zOutput.pos())) { outputBuffer.get(it) }
		println("compressed array is ${compressedArray.contentToString()}")
		println("simple com array is ${ContentWrapper.compress(byteArrayOf(12), 5).contentToString()}")

		val testFile = File("test")
		val compressionStream = CompressingOutputStream(testFile, 5)
		compressionStream.write(byteArrayOf(12))
		//compressionStream.flush()
		compressionStream.close()
		println("from file is ${Files.readAllBytes(testFile.toPath()).contentToString()}")
		assertArrayEquals(byteArrayOf(12), ContentWrapper.decompress(Files.readAllBytes(testFile.toPath())))
	}

	@Test
	fun testMySanity3() {
		val zStream = Zstd.ZSTD_createCStream()
		ZSTD_CCtx_setParameter(zStream, ZSTD_c_compressionLevel, 5)

		val inputBuffer = BufferUtils.createByteBuffer(2)
		inputBuffer.put(0, 12)
		inputBuffer.put(1, 13)
		val zInput = ZSTDInBuffer.create()
		zInput.set(inputBuffer, 0)
		ZSTDInBuffer.nsize(zInput.address(), 1)

		val outputBuffer = BufferUtils.createByteBuffer(20)
		val zOutput = ZSTDOutBuffer.create()
		zOutput.set(outputBuffer, 0)
		assertEquals(0, Zstd.ZSTD_compressStream2(zStream, zOutput, zInput, Zstd.ZSTD_e_end))

		ZSTDInBuffer.nsize(zInput.address(), 2)
		assertEquals(0, Zstd.ZSTD_compressStream2(zStream, zOutput, zInput, Zstd.ZSTD_e_end))

		val compressedArray = ByteArray(toIntExact(zOutput.pos())) { outputBuffer.get(it) }
		println("compressed array is ${compressedArray.contentToString()}")
		println("simple com array is ${ContentWrapper.compress(byteArrayOf(12, 13), 5).contentToString()}")

		assertArrayEquals(byteArrayOf(12, 13), ContentWrapper.decompress(compressedArray))
	}
}
