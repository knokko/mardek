package mardek.importer.ui

import mardek.content.sprite.KimSprite
import mardek.content.ui.UiSprites
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.compressKimSprite2
import mardek.importer.util.compressKimSprite3
import mardek.importer.util.loadBc7Sprite
import javax.imageio.ImageIO

private fun importKimSprite(name: String, bitsPerPixel: Int): KimSprite {
	val path = "mardek/importer/ui/$name.png"
	val resource = BcPacker::class.java.classLoader.getResource(path) ?: throw IllegalArgumentException("Can't load $path")
	val image = ImageIO.read(resource)

	return if (bitsPerPixel == 0) compressKimSprite1(image)
	else if (bitsPerPixel == -1) compressKimSprite3(image)
	else compressKimSprite2(image, bitsPerPixel)
}

private fun importBcSprite(name: String) = loadBc7Sprite("mardek/importer/ui/$name.png")

internal fun importUiSprites() = UiSprites(
	attackIcon = importBcSprite("AttackIcon"),
	defIcon = importBcSprite("DefIcon"),
	rangedDefIcon = importBcSprite("RangedDefIcon"),
	activeStarIcon = importBcSprite("ActiveStarIcon"),
	meleeAttackIcon = importBcSprite("MeleeAttackIcon"),
	rangedAttackIcon = importBcSprite("RangedAttackIcon"),
	meleeDefenseIcon = importBcSprite("MeleeDefenseIcon"),
	rangedDefenseIcon = importBcSprite("RangedDefenseIcon"),
	passiveIcon = importBcSprite("PassiveIcon"),
	goldIcon = importKimSprite("Gold", -1),
	mapChest = importKimSprite("MapChest", -1),
	mapSaveCrystal = importBcSprite("MapSaveCrystal"),
	mapDreamCircle = importBcSprite("MapDreamCircle"),
	skillToggled = importBcSprite("SkillToggled"),
	skillNotToggled = importBcSprite("SkillNotToggled"),
	pointer = importBcSprite("HorizontalPointer"),
	titleScreenBackground = importBcSprite("TitleScreenBackground"),
	blueAlertBalloon = importKimSprite("BlueAlertBalloon", -1),
	redAlertBalloon = importKimSprite("RedAlertBalloon", -1),
	consumableIcon = importKimSprite("Consumable", -1),
	waitIcon = importKimSprite("Wait", -1),
	fleeIcon = importKimSprite("Flee", -1),
	challengeCursor = importBcSprite("ChallengeCursor"),
	dreamStoneIcon = importKimSprite("DreamStone", -1),
	clock = importBcSprite("Clock"),
	arrowHead = importBcSprite("ArrowHead"),
	statusRemoveBackground = importBcSprite("StatusRemoveBackground"),
)
