package mardek.importer.area

import mardek.assets.area.AreaCharacterModel
import javax.imageio.ImageIO

fun importAreaCharacterModel(name: String): AreaCharacterModel {
	val imageInput = AreaImporter::class.java.getResourceAsStream("characters/$name.png")
	val sheet = ImageIO.read(imageInput)
	imageInput.close()

	if (sheet.height != 16 || sheet.width % 16 != 0 || sheet.width < 8 * 16) {
		throw IllegalArgumentException("Invalid size ${sheet.width} x ${sheet.height}")
	}

	val numSprites = sheet.width / 16
	fun extractSprites(offsetX: Int, amount: Int) = (offsetX until offsetX + amount).map { x ->
		sheet.getSubimage(16 * x, 0, 16, 16)
	}

	return AreaCharacterModel(
		name, extractSprites(0, 2),
		extractSprites(2, 2),
		extractSprites(4, 2),
		extractSprites(6, 2),
		extractSprites(8, numSprites - 8)
	)
}
