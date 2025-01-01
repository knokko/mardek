package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class Skeleton(

	@BitField(ordering = 0)
	val animations: HashMap<String, Animation>,

	@BitField(ordering = 1)
	@ReferenceFieldTarget(label = "skeleton parts")
	val parts: Array<SkeletonPart>,
) {

	constructor() : this(HashMap(), emptyArray())
}
