package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class CreatureType(
	@BitField(ordering = 0)
	val flashName: String
) {

	internal constructor() : this("")

	override fun toString() = flashName
}
