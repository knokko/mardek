package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class UiSprites(
	@BitField(id = 0)
	val attackIcon: KimSprite,

	@BitField(id = 1)
	val defIcon: KimSprite,

	@BitField(id = 2)
	val rangedDefIcon: KimSprite,

	@BitField(id = 3)
	val activeStarIcon: KimSprite,

	@BitField(id = 4)
	val meleeAttackIcon: KimSprite,

	@BitField(id = 5)
	val rangedAttackIcon: KimSprite,

	@BitField(id = 6)
	val meleeDefenseIcon: KimSprite,

	@BitField(id = 7)
	val rangedDefenseIcon: KimSprite,

	@BitField(id = 8)
	val passiveIcon: KimSprite,

	@BitField(id = 9)
	val goldIcon: KimSprite,

	@BitField(id = 10)
	val mastered: KimSprite,

	@BitField(id = 11)
	val treasure: KimSprite,

	@BitField(id = 12)
	val plotItem: KimSprite,

	@BitField(id = 13)
	val mapChest: KimSprite,

	@BitField(id = 14)
	val mapSaveCrystal: KimSprite,

	@BitField(id = 15)
	val mapDreamCircle: KimSprite,

	@BitField(id = 16)
	val skillToggled: KimSprite,

	@BitField(id = 17)
	val skillNotToggled: KimSprite,

	@BitField(id = 18)
	val horizontalPointer: KimSprite,

	@BitField(id = 19)
	val verticalPointer: KimSprite,

	@BitField(id = 20)
	val diagonalPointer: KimSprite,

	@BitField(id = 21)
	val titleScreenBackground: BcSprite,

	@BitField(id = 22)
	val titleScreenTitle: BcSprite,

	@BitField(id = 23)
	val blueAlertBalloon: KimSprite,

	@BitField(id = 24)
	val redAlertBalloon: KimSprite,

	@BitField(id = 25)
	val consumableIcon: KimSprite,

	@BitField(id = 26)
	val waitIcon: KimSprite,

	@BitField(id = 27)
	val fleeIcon: KimSprite,
) {

	internal constructor() : this(
		KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		BcSprite(), BcSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite()
	)

	fun allKimSprites() = arrayOf(
		attackIcon, defIcon, rangedDefIcon, activeStarIcon,
		meleeAttackIcon, rangedAttackIcon,
		meleeDefenseIcon, rangedDefenseIcon, passiveIcon,
		goldIcon, mastered, treasure, plotItem,
		mapChest, mapSaveCrystal, mapDreamCircle,
		skillToggled, skillNotToggled, horizontalPointer, verticalPointer, diagonalPointer,
		blueAlertBalloon, redAlertBalloon,
		consumableIcon, waitIcon, fleeIcon
	)

	fun allBcSprites() = arrayOf(titleScreenBackground, titleScreenTitle)
}
