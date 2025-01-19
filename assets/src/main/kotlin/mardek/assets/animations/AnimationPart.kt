package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class AnimationPart(

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "skeleton parts")
	val part: SkeletonPart,

	@BitField(ordering = 1)
	val matrix: AnimationMatrix,
	// TODO color
) {

	constructor() : this(SkeletonPart(), AnimationMatrix())
}
