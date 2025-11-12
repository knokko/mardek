package mardek.importer.util

import com.github.knokko.boiler.utilities.ImageCoding
import com.github.knokko.compressor.Kim1Compressor
import com.github.knokko.compressor.Kim2Compressor
import com.github.knokko.vk2d.Kim3Compressor
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import org.lwjgl.BufferUtils
import java.awt.image.BufferedImage
import java.nio.IntBuffer
import javax.imageio.ImageIO

fun compressKimSprite1(image: BufferedImage) = run {
	val pixelBuffer = BufferUtils.createByteBuffer(4 * image.width * image.height)
	ImageCoding.encodeBufferedImage(pixelBuffer, image)
	pixelBuffer.flip()

	val compressor = Kim1Compressor(pixelBuffer, image.width, image.height, 4)
	val spriteBuffer = BufferUtils.createByteBuffer(4 * compressor.intSize)
	compressor.compress(spriteBuffer)
	KimSprite(IntArray(compressor.intSize) { index -> spriteBuffer.getInt(4 * index) }, 1)
}

fun compressKimSprite3(image: BufferedImage) = run {
	val pixelBuffer = BufferUtils.createByteBuffer(4 * image.width * image.height)
	ImageCoding.encodeBufferedImage(pixelBuffer, image)
	pixelBuffer.flip()

	val compressor = Kim3Compressor(pixelBuffer, image.width, image.height)
	val spriteBuffer = BufferUtils.createByteBuffer(4 * compressor.intSize)
	compressor.compress(spriteBuffer)
	KimSprite(IntArray(compressor.intSize) { index -> spriteBuffer.getInt(4 * index) }, 3)
}

fun compressKimSprite2(image: BufferedImage, bitsPerPixel: Int): KimSprite {
	val outputArray = IntArray(Kim2Compressor.predictIntSize(image.width, image.height, bitsPerPixel))
	Kim2Compressor.compress(image, IntBuffer.wrap(outputArray), bitsPerPixel)
	return KimSprite(outputArray, 2)
}

fun loadBc7Sprite(path: String): BcSprite {
	val resource = classLoader.getResource(path) ?: throw IllegalArgumentException("Can't load $path")
	val image = ImageIO.read(resource)

	val sprite = BcSprite(image.width, image.height, 7)
	sprite.bufferedImage = image
	return sprite
}
