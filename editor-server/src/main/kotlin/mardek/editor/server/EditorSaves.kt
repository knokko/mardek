package mardek.editor.server

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.io.BitCountStream
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import mardek.content.BITSER
import mardek.content.Content
import mardek.exporting.ContentWrapper
import mardek.exporting.DecompressingInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

private val contentFile = File(
	"${Content.RESOURCES_DIRECTORY}/../editor-server/content.bits"
)
private val compressedContentFile = File(
	"${Content.RESOURCES_DIRECTORY}/../editor-server/content-compressed.bits.tmp"
)

fun loadContent(): ContentWrapper {
	val contentInput = BitInputStream(DecompressingInputStream(contentFile))
	val wrappedContent = BITSER.deserialize(
		ContentWrapper::class.java, contentInput, Bitser.BACKWARD_COMPATIBLE,
	)

	println("Start warming up...")
	val warmupTime = measureNanoTime {
		BITSER.serialize(wrappedContent, BitCountStream())
	}
	println("Warming up took ${warmupTime / 1000_000} ms")

	System.gc()
	return wrappedContent
}

fun saveContent(wrappedContent: ContentWrapper, callback: ((ByteArray) -> Unit)?) {
	// TODO BITSER Put back-ups in a different file
	println("start saving content...")
	val saveDuration = measureNanoTime {
		synchronized(compressedContentFile) {
			val output = BitOutputStream(Files.newOutputStream(compressedContentFile.toPath()))
			BITSER.serialize(wrappedContent, output, Bitser.BACKWARD_COMPATIBLE)
			output.finish()
		}
		System.gc()
	}
	println("saving took ${saveDuration / 1_000_000} ms")

	Thread {
		var bytes = ByteArray(0)

		val compressDuration = measureNanoTime {
			val tempContentFile = Files.createTempFile("", ".bits").toFile()
			tempContentFile.deleteOnExit()

			val compressionProcessBuilder = ProcessBuilder(
				"zstd",
				compressedContentFile.absolutePath,
				"-o",
				tempContentFile.absolutePath,
				"-10",
				"--force",
				"--rm",
				"--no-progress",
				"--quiet",
			)
			compressionProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
			compressionProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)

			synchronized(compressedContentFile) {
				val compressionProcess = compressionProcessBuilder.start()
				compressionProcess.waitFor(1, TimeUnit.MINUTES)
				if (compressionProcess.exitValue() != 0) {
					throw RuntimeException("Compression failed")
				}
			}

			synchronized(contentFile) {
				Files.move(
					tempContentFile.toPath(),
					contentFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING,
				)

				if (callback != null) {
					bytes = Files.readAllBytes(contentFile.toPath())
				}
			}

		}

		println("Compression after save took ${compressDuration / 1_000_000} ms")
		if (callback != null) {
			callback(bytes)
		}

		System.gc()

	}.start()
}
