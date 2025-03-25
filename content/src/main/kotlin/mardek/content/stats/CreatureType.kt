package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class CreatureType(
	@BitField(id = 0)
	val flashName: String
) {

	internal constructor() : this("")

	override fun toString() = flashName
}
