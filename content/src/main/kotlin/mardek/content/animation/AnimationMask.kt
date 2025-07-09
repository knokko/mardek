package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.animation.AnimationMatrix

@BitStruct(backwardCompatible = true)
class AnimationMask(

	@BitField(id = 0)
	val sprite: AnimationSprite,

	@BitField(id = 1)
	val matrix: AnimationMatrix,
) {
	@Suppress("unused")
	private constructor() : this(AnimationSprite(), AnimationMatrix())
}
