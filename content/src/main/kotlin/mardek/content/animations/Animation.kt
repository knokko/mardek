package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class Animation(

	@BitField(id = 0)
	val frames: Array<AnimationFrame>,
) {

	@Suppress("unused")
	private constructor() : this(emptyArray())
}
