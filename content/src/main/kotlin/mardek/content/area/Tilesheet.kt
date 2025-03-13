package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class Tilesheet(
	@BitField(id = 0)
	val name: String
) {
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "tiles")
	val tiles = ArrayList<Tile>()

	@BitField(id = 2)
	val waterSprites = ArrayList<KimSprite>()

	internal constructor() : this("")
}
