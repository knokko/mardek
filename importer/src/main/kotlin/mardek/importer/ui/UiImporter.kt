package mardek.importer.ui

import mardek.assets.sprite.KimSprite
import mardek.assets.ui.UiSprites
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.compressKimSprite2
import javax.imageio.ImageIO

private fun importKimSprite(name: String, bitsPerPixel: Int): KimSprite {
	val path = "mardek/importer/ui/$name.png"
	val resource = UiPacker::class.java.classLoader.getResource(path) ?: throw IllegalArgumentException("Can't load $path")
	val image = ImageIO.read(resource)

	return if (bitsPerPixel == 0) compressKimSprite1(image) else compressKimSprite2(image, bitsPerPixel)
}

internal fun importUiSprites() = UiSprites(
	attackIcon = importKimSprite("AttackIcon", 2),
	defIcon = importKimSprite("DefIcon", 2),
	rangedDefIcon = importKimSprite("RangedDefIcon", 2),
	meleeAttackIcon = importKimSprite("MeleeAttackIcon", 2),
	rangedAttackIcon = importKimSprite("RangedAttackIcon", 0),
	meleeDefenseIcon = importKimSprite("MeleeDefenseIcon", 2),
	rangedDefenseIcon = importKimSprite("RangedDefenseIcon", 0),
	passiveIcon = importKimSprite("PassiveIcon", 4),
	goldIcon = importKimSprite("Gold", 0),
	mastered = importKimSprite("Mastered", 0),
	treasure = importKimSprite("TreasureText", 0),
	plotItem = importKimSprite("PlotItemText", 0),
	mapChest = importKimSprite("MapChest", 0),
	mapSaveCrystal = importKimSprite("MapSaveCrystal", 0),
	mapDreamCircle = importKimSprite("MapDreamCircle", 0),
)
