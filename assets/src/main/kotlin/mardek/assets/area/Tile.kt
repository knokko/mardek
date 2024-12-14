package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.assets.sprite.KimSprite

@BitStruct(backwardCompatible = false)
class Tile(
	@BitField(ordering = 0)
	val sprites: ArrayList<KimSprite>,

	@BitField(ordering = 1)
	val canWalkOn: Boolean,

	@BitField(ordering = 2)
	val waterType: WaterType
) {

	@Suppress("unused")
	private constructor() : this(ArrayList(0), false, WaterType.None)
}
