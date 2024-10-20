package mardek.assets.area

import java.awt.image.BufferedImage

class AreaCharacterModel(
	val name: String,
	val downSprites: List<BufferedImage>,
	val upSprites: List<BufferedImage>,
	val rightSprites: List<BufferedImage>,
	val leftSprites: List<BufferedImage>,
	val extraSprites: List<BufferedImage>
) {
	val allSprites = downSprites + upSprites + rightSprites + leftSprites + extraSprites
}
