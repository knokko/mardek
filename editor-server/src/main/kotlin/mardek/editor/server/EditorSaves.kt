package mardek.editor.server

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.io.BitCountStream
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.vk2d.resource.CompressingOutputStream
import com.github.knokko.vk2d.resource.DecompressingInputStream
import mardek.content.BITSER
import mardek.content.Content
import mardek.exporting.ContentWrapper
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.measureNanoTime
import kotlin.time.measureTime

private val contentFile = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content.bits")

fun loadContent(): ContentWrapper {
	val contentInput = BitInputStream(DecompressingInputStream(contentFile.toPath()))
	val wrappedContent = BITSER.deserialize(
		ContentWrapper::class.java, contentInput, Bitser.BACKWARD_COMPATIBLE,
	)
	contentInput.close()

	println("Start warming up...")
	val warmupTime = measureNanoTime {
		BITSER.serialize(wrappedContent, BitCountStream())
	}
	println("Warming up took ${warmupTime / 1000_000} ms")

	System.gc()
	return wrappedContent
}

fun saveContent(wrappedContent: ContentWrapper) {
	println("start saving content...")
	val saveDuration = measureTime {
		val tempFile = Files.createTempFile("backup", ".bits")
		tempFile.toFile().deleteOnExit()
		val output = BitOutputStream(CompressingOutputStream(tempFile, 1))
		BITSER.serialize(wrappedContent, output, Bitser.BACKWARD_COMPATIBLE)
		output.finish()
		Files.move(tempFile, contentFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
		System.gc()
	}
	println("saving took $saveDuration")
}
