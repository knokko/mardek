package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class DirectionalSprites(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val sprites: Array<KimSprite>
) {

	override fun toString() = name

	constructor() : this("", emptyArray())
}
