package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class UiSprites(
	@BitField(id = 0)
	val attackIcon: BcSprite,

	@BitField(id = 1)
	val defIcon: BcSprite,

	@BitField(id = 2)
	val rangedDefIcon: BcSprite,

	@BitField(id = 3)
	val activeStarIcon: BcSprite,

	@BitField(id = 4)
	val meleeAttackIcon: BcSprite,

	@BitField(id = 5)
	val rangedAttackIcon: BcSprite,

	@BitField(id = 6)
	val meleeDefenseIcon: BcSprite,

	@BitField(id = 7)
	val rangedDefenseIcon: BcSprite,

	@BitField(id = 8)
	val passiveIcon: BcSprite,

	@BitField(id = 9)
	val goldIcon: KimSprite,

	@BitField(id = 10)
	val mapChest: KimSprite,

	@BitField(id = 11)
	val mapSaveCrystal: BcSprite,

	@BitField(id = 12)
	val mapDreamCircle: BcSprite,

	@BitField(id = 13)
	val skillToggled: BcSprite,

	@BitField(id = 14)
	val skillNotToggled: BcSprite,

	@BitField(id = 15)
	val pointer: BcSprite,

	@BitField(id = 16)
	val titleScreenBackground: BcSprite,

	@BitField(id = 17)
	val blueAlertBalloon: KimSprite,

	@BitField(id = 18)
	val redAlertBalloon: KimSprite,

	@BitField(id = 19)
	val consumableIcon: KimSprite,

	@BitField(id = 20)
	val waitIcon: KimSprite,

	@BitField(id = 21)
	val fleeIcon: KimSprite,

	@BitField(id = 22)
	val challengeCursor: BcSprite,

	@BitField(id = 23)
	val dreamStoneIcon: KimSprite,

	@BitField(id = 24)
	val clock: BcSprite,

	@BitField(id = 25)
	val arrowHead: BcSprite,
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
	)

	fun allKimSprites() = arrayOf(
		goldIcon, mapChest, blueAlertBalloon, redAlertBalloon,
		consumableIcon, waitIcon, fleeIcon, dreamStoneIcon
	)

	fun allBcSprites() = arrayOf(
		attackIcon, defIcon, rangedDefIcon,
		activeStarIcon, meleeAttackIcon, rangedAttackIcon, meleeDefenseIcon, rangedDefenseIcon, passiveIcon,
		mapSaveCrystal, mapDreamCircle, skillToggled, skillNotToggled, pointer,
		titleScreenBackground, challengeCursor, clock, arrowHead,
	)
}
