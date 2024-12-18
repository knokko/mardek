package mardek.assets.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class ArrowSprite(

	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 1)
	val sprite: KimSprite,
) {

	@Suppress("unused")
	private constructor() : this("", KimSprite())
}
