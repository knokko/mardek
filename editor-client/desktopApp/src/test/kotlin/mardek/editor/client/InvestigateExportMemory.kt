package mardek.editor.client

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.memory.MemorySnapshot
import mardek.content.BITSER
import mardek.exporting.ContentWrapper
import mardek.exporting.DecompressingInputStream
import mardek.exporting.exportAndPrepareVk2d
import java.io.BufferedOutputStream
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files

fun main() {
	println("Initial memory:")
	MemorySnapshot.take().debugDump()

	val input = File("/home/knokko/mardek/editor-server/content.bits")
	val fatContent = BITSER.deserialize(
		ContentWrapper::class.java,
		BitInputStream( DecompressingInputStream(input)),
		Bitser.BACKWARD_COMPATIBLE,
	)
	println("After loading fat content:")
	MemorySnapshot.take().debugDump()

	exportAndPrepareVk2d(
		BufferedOutputStream(Files.newOutputStream(File("test.vk2d").toPath())),
		true, null, fatContent.content
	)

	println("After exporting:")
	MemorySnapshot.take().debugDump()

	System.gc()
	sleep(5000)
	System.gc()
	println("After gc:")
	MemorySnapshot.take().debugDump()
}
