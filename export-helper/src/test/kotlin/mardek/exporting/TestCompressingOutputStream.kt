
package mardek.exporting

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.random.Random

class TestCompressingOutputStream {

	private fun runTest(payload: ByteArray) {
		val testFile = Files.createTempFile("", "")
		testFile.toFile().deleteOnExit()

		val output = CompressingOutputStream(testFile.toFile(), 5)
		output.write(payload)
		output.flush()
		output.close()

		val input = DecompressingInputStream(testFile.toFile())
		assertArrayEquals(payload, input.readAllBytes())
		input.close()
		testFile.deleteIfExists()
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
