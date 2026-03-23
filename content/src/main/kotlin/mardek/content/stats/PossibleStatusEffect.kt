package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

/**
 * A tuple (status effect, chance). This is typically used by skills that may or may not inflict status effects.
 */
@BitStruct(backwardCompatible = true)
class PossibleStatusEffect(

	/**
	 * The status effect that may be given to the target
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "status effects")
	val effect: StatusEffect,

	/**
	 * The base chance that the status effect will be given to the target.
	 *
	 * Note that the elemental resistance of the target against this status effect also plays a role. For instance,
	 * if this chance is 80% and the target has 40% resistance against the status effect, the final chance to give the
	 * status effect would be 0.8 * 0.6 = 48%.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val chance: Int
) {

	@Suppress("unused")
	private constructor() : this(StatusEffect(), 0)

	override fun toString() = "$chance% $effect"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
