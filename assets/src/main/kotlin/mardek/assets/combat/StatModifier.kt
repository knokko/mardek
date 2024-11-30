package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class StatModifier(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "stats")
	val stat: CombatStat,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false)
	val adder: Int,
) {
	override fun toString() = "$stat+$adder"
}
