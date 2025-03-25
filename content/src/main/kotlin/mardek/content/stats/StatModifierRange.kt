package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class StatModifierRange(
	@BitField(id = 0)
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
	private constructor() : this(CombatStat.Attack, 0, 0)

	override fun toString() = "[$minAdder, $maxAdder] $stat"

	operator fun unaryMinus() = StatModifierRange(stat, -maxAdder, -minAdder)
}
