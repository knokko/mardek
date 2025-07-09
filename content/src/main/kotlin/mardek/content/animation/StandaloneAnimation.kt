package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class StandaloneAnimation(
	@BitField(id = 0)
	val frames: AnimationFrames,

	@BitField(id = 1)
	@Suppress("unused")
	@ReferenceFieldTarget(label = "animation sprites")
	val innerSprites: Array<AnimationSprite>,

	@BitField(id = 2)
	@Suppress("unused")
	@ReferenceFieldTarget(label = "skinned animations")
	private val innerAnimations: Array<SkinnedAnimation>,
) {
	constructor() : this(AnimationFrames(), emptyArray(), emptyArray())
}
