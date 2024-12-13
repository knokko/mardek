package mardek.assets.area

import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.assets.sprite.KimSprite

class Tilesheet(
	@BitField(ordering = 0)
	val name: String
) {
	@BitField(ordering = 1)
	@ReferenceFieldTarget(label = "tiles")
	val tiles = ArrayList<Tile>()

	@BitField(ordering = 2)
	val waterSprites = ArrayList<KimSprite>()

	internal constructor() : this("")
}
