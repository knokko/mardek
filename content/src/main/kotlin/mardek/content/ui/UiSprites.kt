package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite

/**
 * The fixed/standard sprites that the engine requires to render the in-game UI.
 */
@BitStruct(backwardCompatible = true)
class UiSprites(

	/**
	 * The attack icon, which is a sword icon
	 */
	@BitField(id = 0)
	val attackIcon: BcSprite,

	/**
	 * The melee defense icon, which is a shield icon
	 */
	@BitField(id = 1)
	val defIcon: BcSprite,

	/**
	 * The magic/ranged defense icon, which is some kind of magic orb icon
	 */
	@BitField(id = 2)
	val rangedDefIcon: BcSprite,

	/**
	 * The star icon that is used to represent active skills
	 */
	@BitField(id = 3)
	val activeStarIcon: BcSprite,

	/**
	 * The icon used to represent melee attack reaction skills, which is a sword icon
	 */
	@BitField(id = 4)
	val meleeAttackIcon: BcSprite,

	/**
	 * The icon used to represent magic/ranged attack reaction skills, which is some kind of light-blue star
	 */
	@BitField(id = 5)
	val rangedAttackIcon: BcSprite,

	/**
	 * The icon used to represent melee defense reaction skills, which is a sword with a 45 degrees square
	 * background
	 */
	@BitField(id = 6)
	val meleeDefenseIcon: BcSprite,

	/**
	 * The icon used to represent magic/ranged defense reaction skills, which is some kind of blue star with a
	 * 45 degrees square background
	 */
	@BitField(id = 7)
	val rangedDefenseIcon: BcSprite,

	/**
	 * The icon used to represent passive skills, which is a green cone
	 */
	@BitField(id = 8)
	val passiveIcon: BcSprite,

	/**
	 * The icon used to represent the amount of gold that the player has
	 */
	@BitField(id = 9)
	val goldIcon: KimSprite,

	/**
	 * The chest icon that is used on the area map tab
	 */
	@BitField(id = 10)
	val mapChest: KimSprite,

	/**
	 * The save crystal icon that is used on the area map tab
	 */
	@BitField(id = 11)
	val mapSaveCrystal: BcSprite,

	/**
	 * The dream circle icon that is used on the area map tab
	 */
	@BitField(id = 12)
	val mapDreamCircle: BcSprite,

	/**
	 * The toggle/checkbox that is shown in the Skills tab when a reaction skill or passive skill is toggled/enabled.
	 */
	@BitField(id = 13)
	val skillToggled: BcSprite,

	/**
	 * The toggle/checkbox that is shown in the Skills tab when a reaction skill or passive skill
	 * is not toggled/enabled.
	 */
	@BitField(id = 14)
	val skillNotToggled: BcSprite,

	/**
	 * The save crystal sprite that is used as 'pointer' in several tabs in the in-game menu. For instance, it points
	 * to the selected character in the inventory tab.
	 */
	@BitField(id = 15)
	val pointer: BcSprite,

	/**
	 * The background sprite of the title screen.
	 */
	@BitField(id = 16)
	val titleScreenBackground: BcSprite,

	/**
	 * The blue alert balloon (exclamation mark) that is shown when the player encounters a random battle that can be
	 * evaded by quickly pressing cancel (Z or Q)
	 */
	@BitField(id = 17)
	val blueAlertBalloon: KimSprite,

	/**
	 * The red alert balloon (exclamation) mark that is shown when the player encounters a random battle that
	 * *cannot* be evaded by pressing Z or Q.
	 */
	@BitField(id = 18)
	val redAlertBalloon: KimSprite,

	/**
	 * The potion icon is shown in the "Items" button of the battle UI when the player is selecting
	 * a move for a character.
	 */
	@BitField(id = 19)
	val consumableIcon: KimSprite,

	/**
	 * The "..." icon that is shown in the "Wait" button of the battle UI when the player is selecting
	 * a move for a character.
	 */
	@BitField(id = 20)
	val waitIcon: KimSprite,

	/**
	 * The 'run away' icon that is shown in the "Flee" button of the battle UI when the player is selecting
	 * a move for a character (except in boss/plot battles where running away is not allowed).
	 */
	@BitField(id = 21)
	val fleeIcon: KimSprite,

	/**
	 * The reaction challenge cursor/caret that is shown during the reaction challenge for reaction skills
	 */
	@BitField(id = 22)
	val challengeCursor: BcSprite,

	/**
	 * The dreamstone icon that is shown in the battle loot screen when an enemy has dropped a dreamstone as loot.
	 */
	@BitField(id = 23)
	val dreamStoneIcon: KimSprite,

	/**
	 * The clock icon that is shown next to the in-game time in some menu's.
	 */
	@BitField(id = 24)
	val clock: BcSprite,

	/**
	 * The 'arrow head' icon that is shown in some menu's to indicate that the player can scroll with the arrow keys
	 * or with AWSD.
	 */
	@BitField(id = 25)
	val arrowHead: BcSprite,

	/**
	 * When a combatant loses a status effect during combat, the game displays a red cross on top of the icon of the
	 * lost status effect. Furthermore, this 'circle' sprite is shown *behind* the status effect icon.
	 */
	@BitField(id = 26)
	val statusRemoveBackground: BcSprite,

	/**
	 * The icon that will be displayed in the Quests tab of the in-game menu. It is displayed on the left of each
	 * quest.
	 */
	@BitField(id = 27)
	val questIcon: BcSprite,

	/**
	 * The closed thrash icon that is shown on some inventory-related menus (when the mouse is *not* hovering over the
	 * thrash icon).
	 */
	@BitField(id = 28)
	val closedThrashIcon: KimSprite,

	/**
	 * The open thrash icon that is shown on some inventory-related menus when the mouse is hovering over the thrash
	 * icon while holding an item.
	 */
	@BitField(id = 29)
	val openThrashIcon: KimSprite,

	/**
	 * The sort icon that is shown on some inventory-related UI's.
	 */
	@BitField(id = 30)
	val sortIcon1: KimSprite,

	/**
	 * A slight variation of [sortIcon1]
	 */
	@BitField(id = 31)
	val sortIcon2: KimSprite,

	/**
	 * The parchment/scroll sprite that is shown on the world map, behind the display name of the selected area.
	 */
	@BitField(id = 32)
	val worldMapScroll: BcSprite,

	/**
	 * The orange circle node sprite that is shown at the world map node where the player resides
	 */
	@BitField(id = 33)
	val worldMapCurrentArea: BcSprite,

	/**
	 * The red circle node sprite that is shown at each discovered world map node where the player does *not* currently
	 * reside.
	 */
	@BitField(id = 34)
	val worldMapDiscoveredArea: BcSprite,

	/**
	 * The grey circle node sprite that is shown at each world map node that is currently blocked (e.g. most nodes are
	 * blocked during the date with Elwyen at the end of chapter 3).
	 */
	@BitField(id = 35)
	val worldMapBlockedArea: BcSprite,

	/**
	 * The icon for the "People" section of the encyclopedia
	 */
	@BitField(id = 36)
	val encyclopediaPeople: KimSprite,

	/**
	 * The icon for the "Places" section of the encyclopedia
	 */
	@BitField(id = 37)
	val encyclopediaPlaces: KimSprite,

	/**
	 * The icon for the "Artefacts" section of the encyclopedia
	 */
	@BitField(id = 38)
	val encyclopediaArtefacts: KimSprite,

	/**
	 * The icon for the "Bestiary" section of the encyclopedia
	 */
	@BitField(id = 39)
	val encyclopediaBestiary: KimSprite,

	/**
	 * The icon for the "Dreamstones" section of the encyclopedia
	 */
	@BitField(id = 40)
	val encyclopediaDreamstones: KimSprite,
) {

	@Suppress("unused")
	private constructor() : this(
		BcSprite(), BcSprite(), BcSprite(),
		BcSprite(), BcSprite(), BcSprite(),
		BcSprite(), BcSprite(), BcSprite(),
		KimSprite(), KimSprite(),
		BcSprite(), BcSprite(),
		BcSprite(), BcSprite(), BcSprite(),
		BcSprite(),
		KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), BcSprite(),
		KimSprite(), BcSprite(), BcSprite(),
		BcSprite(), BcSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		BcSprite(), BcSprite(),
		BcSprite(), BcSprite(),
		KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(),
	)
}
