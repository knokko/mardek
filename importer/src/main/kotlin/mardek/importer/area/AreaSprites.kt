package mardek.importer.area

import com.github.knokko.compressor.Kim1Compressor
import mardek.assets.area.sprites.DirectionalSpritesheet
import mardek.assets.area.sprites.KimImage
import mardek.assets.area.sprites.ObjectSpritesheet
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import java.awt.Color
import javax.imageio.ImageIO

class AreaSprites {

	val characters = mutableListOf<DirectionalSpritesheet>()
	val objects = mutableListOf<ObjectSpritesheet>()

	fun getCharacter(flashName: String): DirectionalSpritesheet {
		for (sheet in characters) {
			if (sheet.flashName == flashName) return sheet
		}

		val input = AreaSprites::class.java.getResourceAsStream("sheets/character/$flashName.png")
		val sheetImage = ImageIO.read(input)
		input.close()

		val numSprites = sheetImage.width / 16

		val sheet = DirectionalSpritesheet(flashName)
		val sprites = (0 until numSprites).map { sheetImage.getSubimage(it * 16, 0, 16, sheetImage.height) }

		val pixelBuffer = memCalloc(4 * 16 * sheetImage.height)

		sheet.sprites = sprites.map { sprite ->
			pixelBuffer.position(0)
			for (y in 0 until sheetImage.height) {
				for (x in 0 until 16) {
					val color = Color(sprite.getRGB(x, y))
					pixelBuffer.put(color.red.toByte())
					pixelBuffer.put(color.green.toByte())
					pixelBuffer.put(color.blue.toByte())
					pixelBuffer.put(color.alpha.toByte())
				}
			}
			pixelBuffer.position(0)
			val compressor = Kim1Compressor(pixelBuffer, 16, sheetImage.height, 4)
			val compressedBuffer = memCalloc(4 * compressor.intSize)
			compressor.compress(compressedBuffer)

			val result = KimImage(IntArray(compressor.intSize) { index -> compressedBuffer.getInt(4 * index)})
			memFree(compressedBuffer)
			result
		}.toTypedArray()

		memFree(pixelBuffer)

		characters.add(sheet)
		return sheet
	}

	fun getObject(flashName: String) {

	}

	fun getObject(flashName: String, offsetY: Int, height: Int) {

	}
}
