package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class SkeletonPart(

	@BitField(id = 0)
	val skins: Array<BodyPart>
) {
	constructor() : this(emptyArray())
}
