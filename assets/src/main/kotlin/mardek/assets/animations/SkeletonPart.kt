package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class SkeletonPart(

	@BitField(ordering = 0)
	val skins: Array<BodyPart>
) {
	constructor() : this(emptyArray())
}
