package mardek.importer.ui

import mardek.content.sprite.KimSprite
import mardek.content.ui.UiSprites
import mardek.importer.util.classLoader
import mardek.importer.util.compressKimSprite3
import mardek.importer.util.loadBc7Sprite
import javax.imageio.ImageIO

private fun importKimSprite(name: String): KimSprite {
	val path = "mardek/importer/ui/$name.png"
	val resource = classLoader.getResource(path) ?: throw IllegalArgumentException("Can't load $path")
	val image = ImageIO.read(resource)

	return compressKimSprite3(image)
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
	goldIcon = importKimSprite("Gold"),
	mapChest = importKimSprite("MapChest"),
	mapSaveCrystal = importBcSprite("MapSaveCrystal"),
	mapDreamCircle = importBcSprite("MapDreamCircle"),
	skillToggled = importBcSprite("SkillToggled"),
	skillNotToggled = importBcSprite("SkillNotToggled"),
	pointer = importBcSprite("HorizontalPointer"),
	titleScreenBackground = importBcSprite("TitleScreenBackground"),
	blueAlertBalloon = importKimSprite("BlueAlertBalloon"),
	redAlertBalloon = importKimSprite("RedAlertBalloon"),
	consumableIcon = importKimSprite("Consumable"),
	waitIcon = importKimSprite("Wait"),
	fleeIcon = importKimSprite("Flee"),
	challengeCursor = importBcSprite("ChallengeCursor"),
	dreamStoneIcon = importKimSprite("DreamStone"),
	clock = importBcSprite("Clock"),
	arrowHead = importBcSprite("ArrowHead"),
	statusRemoveBackground = importBcSprite("StatusRemoveBackground"),
	questIcon = importBcSprite("QuestIcon"),
	closedThrashIcon = importKimSprite("ClosedThrash"),
	openThrashIcon = importKimSprite("OpenThrash"),
	sortIcon1 = importKimSprite("Sort1"),
	sortIcon2 = importKimSprite("Sort2"),
	worldMapScroll = loadBc7Sprite("mardek/importer/world/AreaNameScroll.png"),
	worldMapCurrentArea = loadBc7Sprite("mardek/importer/world/CurrentAreaNode.png"),
	worldMapDiscoveredArea = loadBc7Sprite("mardek/importer/world/DiscoveredAreaNode.png"),
	worldMapBlockedArea = loadBc7Sprite("mardek/importer/world/BlockedAreaNode.png"),
)
