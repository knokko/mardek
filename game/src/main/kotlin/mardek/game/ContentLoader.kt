package mardek.game

import mardek.content.BITSER
import mardek.content.Content
import mardek.content.Content.Companion.RESOURCES_DIRECTORY
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.zstd.Zstd.ZSTD_decompress
import org.lwjgl.util.zstd.Zstd.ZSTD_getErrorName
import org.lwjgl.util.zstd.Zstd.ZSTD_getFrameContentSize
import org.lwjgl.util.zstd.Zstd.ZSTD_isError
import java.io.File
import java.lang.Math.toIntExact
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

fun loadContent(contentFile: File = File("$RESOURCES_DIRECTORY/content.bits")): Content {
	FileChannel.open(contentFile.toPath(), StandardOpenOption.READ).use { channel ->
		val compressedBuffer = memCalloc(toIntExact(channel.size()))
		while (compressedBuffer.position() < compressedBuffer.capacity()) channel.read(compressedBuffer)
		compressedBuffer.position(0)

		val expectedUncompressedSize = ZSTD_getFrameContentSize(compressedBuffer)
		if (ZSTD_isError(expectedUncompressedSize)) {
			throw Error("Invalid compressed content: ${ZSTD_getErrorName(expectedUncompressedSize)}")
		}
		val uncompressedBuffer = memCalloc(toIntExact(expectedUncompressedSize))
		val actualUncompressedSize = ZSTD_decompress(uncompressedBuffer, compressedBuffer)
		memFree(compressedBuffer)
		if (expectedUncompressedSize != actualUncompressedSize) {
			throw Error("Content compressed size mismatch")
		}

		val contentBytes = ByteArray(uncompressedBuffer.capacity())
		uncompressedBuffer.get(contentBytes)
		val content = BITSER.fromBytes(Content::class.java, contentBytes)
		memFree(uncompressedBuffer)
		return content
	}
}
