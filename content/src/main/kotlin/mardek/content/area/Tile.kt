package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class Tile(
	@BitField(id = 0)
	val sprites: ArrayList<KimSprite>,

	@BitField(id = 1)
	val canWalkOn: Boolean,

	@BitField(id = 2)
	val waterType: WaterType
) {

	@Suppress("unused")
	private constructor() : this(ArrayList(0), false, WaterType.None)
}
