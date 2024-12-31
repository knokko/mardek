package mardek.assets.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.assets.sprite.KimSprite

@BitStruct(backwardCompatible = false)
class UiSprites(
	@BitField(ordering = 0)
	val attackIcon: KimSprite,

	@BitField(ordering = 1)
	val defIcon: KimSprite,

	@BitField(ordering = 2)
	val rangedDefIcon: KimSprite,

	@BitField(ordering = 3)
	val activeStarIcon: KimSprite,

	@BitField(ordering = 4)
	val meleeAttackIcon: KimSprite,

	@BitField(ordering = 5)
	val rangedAttackIcon: KimSprite,

	@BitField(ordering = 6)
	val meleeDefenseIcon: KimSprite,

	@BitField(ordering = 7)
	val rangedDefenseIcon: KimSprite,

	@BitField(ordering = 8)
	val passiveIcon: KimSprite,

	@BitField(ordering = 9)
	val goldIcon: KimSprite,

	@BitField(ordering = 10)
	val mastered: KimSprite,

	@BitField(ordering = 11)
	val treasure: KimSprite,

	@BitField(ordering = 12)
	val plotItem: KimSprite,

	@BitField(ordering = 13)
	val mapChest: KimSprite,

	@BitField(ordering = 14)
	val mapSaveCrystal: KimSprite,

	@BitField(ordering = 15)
	val mapDreamCircle: KimSprite,

	@BitField(ordering = 16)
	val skillToggled: KimSprite,

	@BitField(ordering = 17)
	val skillNotToggled: KimSprite,

	@BitField(ordering = 18)
	val horizontalPointer: KimSprite,

	@BitField(ordering = 19)
	val diagonalPointer: KimSprite,
) {

	internal constructor() : this(
		KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite()
	)

	fun allKimSprites() = arrayOf(
		attackIcon, defIcon, rangedDefIcon, activeStarIcon,
		meleeAttackIcon, rangedAttackIcon,
		meleeDefenseIcon, rangedDefenseIcon, passiveIcon,
		goldIcon, mastered, treasure, plotItem,
		mapChest, mapSaveCrystal, mapDreamCircle,
		skillToggled, skillNotToggled, horizontalPointer, diagonalPointer
	)
}
