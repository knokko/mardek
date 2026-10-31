package mardek.importer.util

import com.github.knokko.boiler.utilities.ImageCoding
import com.github.knokko.compressor.Kim1Compressor
import com.github.knokko.compressor.Kim3Compressor
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import org.lwjgl.BufferUtils
import java.awt.image.BufferedImage
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

fun loadBc7Sprite(path: String): BcSprite {
	val resource = classLoader.getResource(path) ?: throw IllegalArgumentException("Can't load $path")
	val image = ImageIO.read(resource)

	val sprite = BcSprite(image.width, image.height, 7, false, 0)
	sprite.bufferedImage = image
	return sprite
}
