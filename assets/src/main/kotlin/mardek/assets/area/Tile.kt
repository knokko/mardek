package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.assets.sprite.KimSprite

@BitStruct(backwardCompatible = false)
class Tile(
	@BitField(ordering = 0)
	val sprites: List<KimSprite>,

	@BitField(ordering = 1)
	val canWalkOn: Boolean,

	@BitField(ordering = 2)
	val waterType: WaterType
) {
}
