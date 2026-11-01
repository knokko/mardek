package mardek.exporting

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.random.Random

class TestDecompressingInputStream {

	private fun runTest(payload: ByteArray) {
		val testFile = Files.createTempFile("", "")
		testFile.toFile().deleteOnExit()

		val compressed = ContentWrapper.compress(payload, 5)

		// Sanity check
		assertArrayEquals(payload, ContentWrapper.decompress(compressed))
		Files.write(testFile, compressed)

		val input = DecompressingInputStream(testFile.toFile())
		val decompressed = input.readAllBytes()
		input.close()

		testFile.deleteIfExists()
		assertArrayEquals(payload, decompressed)
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
}
