package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class AnimationFrame(

	@BitField(ordering = 0)
	val parts: Array<AnimationPart>,
) {

	constructor() : this(emptyArray())
}
