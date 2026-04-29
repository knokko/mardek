package mardek.audio

import mardek.content.Content
import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBVorbis.VORBIS__no_error
import org.lwjgl.stb.STBVorbis.stb_vorbis_close
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_info
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved
import org.lwjgl.stb.STBVorbis.stb_vorbis_open_memory
import org.lwjgl.stb.STBVorbis.stb_vorbis_stream_length_in_samples
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memCallocShort
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.zstd.Zstd.ZSTD_compress
import org.lwjgl.util.zstd.Zstd.ZSTD_compressBound
import org.lwjgl.util.zstd.Zstd.ZSTD_decompress
import org.lwjgl.util.zstd.Zstd.ZSTD_getFrameContentSize
import java.io.File
import java.lang.Math.toIntExact
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

// This is a failed experiment to do the vorbis decoding during exporting rather than during runtime.
// Unfortunately, that blows up the music file sizes by an order of magnitude, even after maximum ZSTD compression.
fun main() {
	val files = File("${Content.RESOURCES_DIRECTORY}/music").listFiles()!!
	for (compressedFile in files.filter { it.name.contains("TitleScreen") }) {
		if (!compressedFile.name.endsWith(".ogg.zstd")) continue
		val originalFile = File(compressedFile.path.replace(".ogg.zstd", ".ogg"))
		val rawFile = File(compressedFile.path.replace(".ogg.zstd", ""))
		val rawCompressedFile = File("$rawFile.zstd")
		if (rawCompressedFile.exists()) continue

		val originalBuffer: ByteBuffer
		FileChannel.open(compressedFile.toPath(), StandardOpenOption.READ).use { inputChannel ->
			val compressed = BufferUtils.createByteBuffer(toIntExact(inputChannel.size()))
			while (compressed.position() < compressed.limit()) inputChannel.read(compressed)
			compressed.position(0)

			val expectedOriginalSize = ZSTD_getFrameContentSize(compressed)
			originalBuffer = memCalloc(toIntExact(expectedOriginalSize))
			val actualOriginalSize = ZSTD_decompress(originalBuffer, compressed)
			memFree(compressed)
			if (expectedOriginalSize != actualOriginalSize) {
				throw Error("Music compressed size mismatch for $compressedFile")
			}
		}

		FileChannel.open(
			originalFile.toPath(), StandardOpenOption.TRUNCATE_EXISTING,
			StandardOpenOption.CREATE, StandardOpenOption.WRITE,
		).use { outputChannel ->
			while (originalBuffer.position() < originalBuffer.limit()) {
				outputChannel.write(originalBuffer)
			}
		}

		originalBuffer.position(0)

		val rawBuffer: ByteBuffer
		MemoryStack.stackPush().use { stack ->
			val error = stack.callocInt(1)
			val decoder = stb_vorbis_open_memory(originalBuffer, error, null)
			if (error[0] != VORBIS__no_error) {
				throw AudioException("stb_vorbis_open_memory($originalFile) caused error ${error[0]}")
			}
			if (decoder == 0L) throw Error("stb_vorbis_open_xxx failed for $originalFile")

			val info = STBVorbisInfo.calloc(stack)
			stb_vorbis_get_info(decoder, info)

			val decodedAudio = memCallocShort(stb_vorbis_stream_length_in_samples(decoder) * info.channels())
			stb_vorbis_get_samples_short_interleaved(decoder, info.channels(), decodedAudio)
			stb_vorbis_close(decoder)

			rawBuffer = ByteBuffer.allocateDirect(2 * decodedAudio.limit())
			while (decodedAudio.remaining() > 0) rawBuffer.putShort(decodedAudio.get())
			rawBuffer.position(0)

			memFree(decodedAudio)
		}

		FileChannel.open(
			rawFile.toPath(), StandardOpenOption.TRUNCATE_EXISTING,
			StandardOpenOption.CREATE, StandardOpenOption.WRITE,
		).use { outputChannel ->
			while (rawBuffer.position() < rawBuffer.limit()) {
				outputChannel.write(rawBuffer)
			}
		}

		rawBuffer.position(0)

		val rawCompressedCapacity = ZSTD_compressBound(rawBuffer.limit().toLong())
		val rawCompressedBuffer = BufferUtils.createByteBuffer(toIntExact(rawCompressedCapacity))
		val rawCompressedSize = ZSTD_compress(rawCompressedBuffer, rawBuffer, 22)
		rawCompressedBuffer.limit(toIntExact(rawCompressedSize))

		FileChannel.open(
			rawCompressedFile.toPath(),
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING,
			StandardOpenOption.WRITE
		).use { outputChannel ->
			while (rawCompressedBuffer.position() < rawCompressedBuffer.limit()) {
				outputChannel.write(rawCompressedBuffer)
			}
		}
	}
}
