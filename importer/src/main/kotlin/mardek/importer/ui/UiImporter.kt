package mardek.importer.ui

import mardek.assets.sprite.KimSprite
import mardek.assets.ui.UiSprites
import mardek.importer.util.compressSprite
import javax.imageio.ImageIO

private fun importKimSprite(name: String): KimSprite {
	val path = "mardek/importer/ui/$name.png"
	val input = UiPacker::class.java.classLoader.getResourceAsStream(path) ?: throw IllegalArgumentException("Can't load $path")

	val image = ImageIO.read(input)
	input.close()

	return compressSprite(image)
}

internal fun importUiSprites() = UiSprites(
	attackIcon = importKimSprite("AttackIcon"),
	defIcon = importKimSprite("DefIcon"),
	rangedDefIcon = importKimSprite("RangedDefIcon"),
	meleeAttackIcon = importKimSprite("MeleeAttackIcon"),
	rangedAttackIcon = importKimSprite("RangedAttackIcon"),
	meleeDefenseIcon = importKimSprite("MeleeDefenseIcon"),
	rangedDefenseIcon = importKimSprite("RangedDefenseIcon"),
	passiveIcon = importKimSprite("PassiveIcon"),
	goldIcon = importKimSprite("Gold"),
	mastered = importKimSprite("Mastered"),
)
