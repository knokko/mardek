package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class AnimationFrame(

	@BitField(id = 0)
	val parts: Array<AnimationPart>,
) {

	@Suppress("unused")
	private constructor() : this(emptyArray())
}
