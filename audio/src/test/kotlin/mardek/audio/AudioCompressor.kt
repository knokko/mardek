package mardek.audio

import mardek.content.Content
import org.lwjgl.BufferUtils
import org.lwjgl.util.zstd.Zstd
import org.lwjgl.util.zstd.Zstd.ZSTD_compress
import java.io.File
import java.lang.Math.toIntExact
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

fun main() {
	val files = File("${Content.RESOURCES_DIRECTORY}/music").listFiles()!!
	for (file in files) {
		val compressedFile = File("$file.zstd")
		if (!file.name.endsWith(".ogg") || compressedFile.exists()) continue
		FileChannel.open(file.toPath(), StandardOpenOption.READ).use { inputChannel ->
			val source = BufferUtils.createByteBuffer(toIntExact(inputChannel.size()))
			while (source.position() < source.limit()) inputChannel.read(source)
			source.position(0)

			val destinationCapacity = Zstd.ZSTD_compressBound(inputChannel.size())
			val destination = BufferUtils.createByteBuffer(toIntExact(destinationCapacity))
			val destinationSize = ZSTD_compress(destination, source, 22)
			destination.limit(toIntExact(destinationSize))

			FileChannel.open(
				compressedFile.toPath(),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
			).use { outputChannel ->
				while (destination.position() < destination.limit()) {
					outputChannel.write(destination)
				}
			}
			file.delete()
		}
	}
}
