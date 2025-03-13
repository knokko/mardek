package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class AnimationPart(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "skeleton parts")
	val part: SkeletonPart,

	@BitField(id = 1)
	val matrix: AnimationMatrix,

	@BitField(id = 2, optional = true)
	val color: ColorTransform?,
) {

	@Suppress("unused")
	constructor() : this(SkeletonPart(), AnimationMatrix(), null)
}
