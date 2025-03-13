package mardek.content.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class StatModifierRange(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "stats")
	val stat: CombatStat,

	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val minAdder: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false)
	val maxAdder: Int
) {

	init {
		if (maxAdder < minAdder) throw IllegalArgumentException("Invalid adder range [$minAdder, $maxAdder")
	}

	@Suppress("unused")
	private constructor() : this(CombatStat(), 0, 0)

	override fun toString() = "[$minAdder, $maxAdder] $stat"

	operator fun unaryMinus() = StatModifierRange(stat, -maxAdder, -minAdder)
}
