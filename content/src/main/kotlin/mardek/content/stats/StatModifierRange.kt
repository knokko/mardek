package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

/**
 * Increases the [stat] of a combatant by a random amount between [minAdder] and [maxAdder] (both inclusive).
 * Note that both [minAdder] and [maxAdder] may or may not be negative, so the [stat] could also be decreased rather
 * than increased.
 */
@BitStruct(backwardCompatible = true)
class StatModifierRange(

	/**
	 * The [CombatStat] of the combatant to be increased (or decreased)
	 */
	@BitField(id = 0)
	val stat: CombatStat,

	/**
	 * The minimum amount by which the [stat] of the combatant will be increased.
	 * Cannot be larger than [maxAdder].
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val minAdder: Int,

	/**
	 * The maximum amount by which the [stat] of the combatant will be increased.
	 * Cannot be smaller than [minAdder].
	 */
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
