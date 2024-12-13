package mardek.importer.util

import com.github.knokko.compressor.Kim1Compressor
import mardek.assets.inventory.Item
import org.lwjgl.system.MemoryStack.stackPush
import java.awt.Color
import java.awt.image.BufferedImage

fun compressSprite(image: BufferedImage) = stackPush().use { stack ->
	val pixelBuffer = stack.calloc(4 * image.width * image.height)
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
	val spriteBuffer = stack.calloc(4 * compressor.intSize)
	compressor.compress(spriteBuffer)
	IntArray(compressor.intSize) { index -> spriteBuffer.getInt(4 * index) }
}
