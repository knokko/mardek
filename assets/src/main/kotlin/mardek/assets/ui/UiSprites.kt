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
	val meleeAttackIcon: KimSprite,

	@BitField(ordering = 4)
	val rangedAttackIcon: KimSprite,

	@BitField(ordering = 5)
	val meleeDefenseIcon: KimSprite,

	@BitField(ordering = 6)
	val rangedDefenseIcon: KimSprite,

	@BitField(ordering = 7)
	val passiveIcon: KimSprite,

	@BitField(ordering = 8)
	val goldIcon: KimSprite,

	@BitField(ordering = 9)
	val mastered: KimSprite,

	@BitField(ordering = 10)
	val treasure: KimSprite,

	@BitField(ordering = 11)
	val plotItem: KimSprite,

	@BitField(ordering = 12)
	val mapChest: KimSprite,

	@BitField(ordering = 13)
	val mapSaveCrystal: KimSprite,

	@BitField(ordering = 14)
	val mapDreamCircle: KimSprite,
) {

	internal constructor() : this(
		KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite(), KimSprite(),
		KimSprite(), KimSprite(), KimSprite()
	)

	fun allKimSprites() = arrayOf(
		attackIcon, defIcon, rangedDefIcon,
		meleeAttackIcon, rangedAttackIcon,
		meleeDefenseIcon, rangedDefenseIcon, passiveIcon,
		goldIcon, mastered, treasure, plotItem,
		mapChest, mapSaveCrystal, mapDreamCircle
	)
}
