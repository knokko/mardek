package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class StatModifierRange(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "stats")
	val stat: CombatStat,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false)
	val minAdder: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false)
	val maxAdder: Int
) {

	init {
		if (maxAdder < minAdder) throw IllegalArgumentException("Invalid adder range [$minAdder, $maxAdder")
	}

	override fun toString() = "[$minAdder, $maxAdder] $stat"

	operator fun unaryMinus() = StatModifierRange(stat, -maxAdder, -minAdder)
}
