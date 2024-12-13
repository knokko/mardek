package mardek.assets.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class DirectionalSprites(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val sprites: Array<KimSprite>
) {

	override fun toString() = name

	internal constructor() : this("", emptyArray())
}
