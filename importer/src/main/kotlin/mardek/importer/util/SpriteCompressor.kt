package mardek.importer.util

import com.github.knokko.compressor.Kim1Compressor
import com.github.knokko.compressor.Kim2Compressor
import mardek.assets.sprite.KimSprite
import org.lwjgl.BufferUtils
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.IntBuffer

fun compressKimSprite1(image: BufferedImage) = run {
	val pixelBuffer = BufferUtils.createByteBuffer(4 * image.width * image.height)
	for (x in 0 until image.width) {
		for (y in 0 until image.height) {
			val color = Color(image.getRGB(x, y), true)
			val index = 4 * (x + y * image.width)
			pixelBuffer.put(index, color.red.toByte())
			pixelBuffer.put(index + 1, color.green.toByte())
			pixelBuffer.put(index + 2, color.blue.toByte())
			pixelBuffer.put(index + 3, color.alpha.toByte())
		}
	}

	val compressor = Kim1Compressor(pixelBuffer, image.width, image.height, 4)
	val spriteBuffer = BufferUtils.createByteBuffer(4 * compressor.intSize)
	compressor.compress(spriteBuffer)
	KimSprite(IntArray(compressor.intSize) { index -> spriteBuffer.getInt(4 * index) }, 1)
}

fun compressKimSprite2(image: BufferedImage, bitsPerPixel: Int): KimSprite {
	val outputArray = IntArray(Kim2Compressor.predictIntSize(image.width, image.height, bitsPerPixel))
	Kim2Compressor.compress(image, IntBuffer.wrap(outputArray), bitsPerPixel)
	return KimSprite(outputArray, 2)
}
