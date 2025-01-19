package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class BodyPart(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val entries: Array<BodyPartEntry>
) {

	constructor() : this("", emptyArray())
}
