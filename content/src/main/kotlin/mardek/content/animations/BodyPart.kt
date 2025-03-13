package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class BodyPart(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val entries: Array<BodyPartEntry>
) {

	@Suppress("unused")
	constructor() : this("", emptyArray())
}
