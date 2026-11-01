package mardek.exporting

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.Content
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.zstd.Zstd.ZSTD_compress
import org.lwjgl.util.zstd.Zstd.ZSTD_compressBound
import org.lwjgl.util.zstd.Zstd.ZSTD_decompress
import org.lwjgl.util.zstd.Zstd.ZSTD_getErrorName
import org.lwjgl.util.zstd.Zstd.ZSTD_getFrameContentSize
import org.lwjgl.util.zstd.Zstd.ZSTD_isError
import java.io.Serializable
import java.lang.Math.toIntExact

@BitStruct(backwardCompatible = true)
class ContentWrapper(

	@BitField(id = 0)
	val content: Content,

	@BitField(id = 1)
	val neutralMusicNote: BcSprite,

	@BitField(id = 2)
	val musicNoteShadow: BcSprite,

	@BitField(id = 3)
	val playMusicIcon: BcSprite,

	@BitField(id = 4)
	val pauseMusicIcon: BcSprite,

	@BitField(id = 5)
	val playMusicHoveredIcon: BcSprite,

	@BitField(id = 6)
	val pauseMusicHoveredIcon: BcSprite,

	@BitField(id = 7)
	val applicationIcon: KimSprite,

	@BitField(id = 8)
	val inventoryCursor: KimSprite,

	@BitField(id = 9)
	val pointerCursor: KimSprite,

	@BitField(id = 10)
	val grabCursor: KimSprite,
) : Serializable {

	companion object {

		fun compress(original: ByteArray, level: Int): ByteArray {
			val uncompressedBuffer = memCalloc(original.size)
			uncompressedBuffer.put(0, original)

			val compressedCapacity = ZSTD_compressBound(original.size.toLong())
			val compressedBuffer = memCalloc(toIntExact(compressedCapacity))
			val compressedSize = ZSTD_compress(compressedBuffer, uncompressedBuffer, level)
			compressedBuffer.limit(toIntExact(compressedSize))

			val compressedBytes = ByteArray(toIntExact(compressedSize))
			compressedBuffer.get(compressedBytes)

			memFree(uncompressedBuffer)
			memFree(compressedBuffer)

			return compressedBytes
		}

		fun decompress(compressed: ByteArray): ByteArray {
			if (compressed.isEmpty()) throw IllegalArgumentException("compressed array can't be empty")

			val compressedBuffer = memCalloc(compressed.size)
			compressedBuffer.put(0, compressed)

			val expectedDecompressedSize = ZSTD_getFrameContentSize(compressedBuffer)
			if (ZSTD_isError(expectedDecompressedSize)) {
				throw IllegalArgumentException("Invalid compressed array ${ZSTD_getErrorName(expectedDecompressedSize)}")
			}

			val decompressedBuffer = memCalloc(toIntExact(expectedDecompressedSize))
			val actualDecompressedSize = ZSTD_decompress(decompressedBuffer, compressedBuffer)
			if (ZSTD_isError(actualDecompressedSize)) {
				throw IllegalArgumentException("Invalid compressed array ${ZSTD_getErrorName(actualDecompressedSize)}")
			}
			if (expectedDecompressedSize != actualDecompressedSize) {
				throw RuntimeException("Size mismatch: expected $expectedDecompressedSize, but got $actualDecompressedSize")
			}

			val decompressed = ByteArray(toIntExact(actualDecompressedSize))
			decompressedBuffer.get(decompressed)

			memFree(compressedBuffer)
			memFree(decompressedBuffer)
			return decompressed
		}
	}
}
