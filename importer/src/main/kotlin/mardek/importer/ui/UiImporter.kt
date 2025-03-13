package mardek.importer.ui

import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.ui.UiSprites
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.compressKimSprite2
import javax.imageio.ImageIO

private fun importKimSprite(name: String, bitsPerPixel: Int): KimSprite {
	val path = "mardek/importer/ui/$name.png"
	val resource = BcPacker::class.java.classLoader.getResource(path) ?: throw IllegalArgumentException("Can't load $path")
	val image = ImageIO.read(resource)

	return if (bitsPerPixel == 0) compressKimSprite1(image) else compressKimSprite2(image, bitsPerPixel)
}

private fun importBcSprite(name: String, version: Int): BcSprite {
	val path = "mardek/importer/ui/$name.png"
	val resource = BcPacker::class.java.classLoader.getResource(path) ?: throw IllegalArgumentException("Can't load $path")
	var image = ImageIO.read(resource)

	if (version == 1 && (image.width % 4 != 0 || image.height % 4 != 0)) {
		image = image.getSubimage(0, 0,  4 * (image.width / 4), 4 * (image.height / 4))
	}

	val sprite = BcSprite(image.width, image.height, version)
	sprite.bufferedImage = image
	return sprite
}

internal fun importUiSprites() = UiSprites(
	attackIcon = importKimSprite("AttackIcon", 2),
	defIcon = importKimSprite("DefIcon", 2),
	rangedDefIcon = importKimSprite("RangedDefIcon", 2),
	activeStarIcon = importKimSprite("ActiveStarIcon", 0),
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
	skillToggled = importKimSprite("SkillToggled", 0),
	skillNotToggled = importKimSprite("SkillNotToggled", 0),
	horizontalPointer = importKimSprite("HorizontalPointer", 0),
	diagonalPointer = importKimSprite("DiagonalPointer", 0),
	titleScreenBackground = importBcSprite("TitleScreenBackground", 1),
	titleScreenTitle = importBcSprite("TitleScreenTitle", 7),
	blueAlertBalloon = importKimSprite("BlueAlertBalloon", 0),
	redAlertBalloon = importKimSprite("RedAlertBalloon", 0),
)
