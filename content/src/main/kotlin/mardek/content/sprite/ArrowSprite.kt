package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class ArrowSprite(

	@BitField(id = 0)
	val flashName: String,

	@BitField(id = 1)
	val sprite: KimSprite,
) {

	@Suppress("unused")
	private constructor() : this("", KimSprite())
}
