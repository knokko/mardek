package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class Animation(

	@BitField(ordering = 0)
	val frames: Array<AnimationFrame>,
) {

	constructor() : this(emptyArray())
}
