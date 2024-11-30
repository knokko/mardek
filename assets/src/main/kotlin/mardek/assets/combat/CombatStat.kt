package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class CombatStat(
	@BitField(ordering = 0)
	val flashName: String,
) {

	override fun toString() = flashName
}
