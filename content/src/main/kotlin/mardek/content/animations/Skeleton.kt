package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class Skeleton(

	@BitField(id = 0)
	val animations: HashMap<String, Animation>,

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "skeleton parts")
	val parts: Array<SkeletonPart>,
) {

	constructor() : this(HashMap(), emptyArray())
}
