package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class PossibleStatusEffect(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "status effects")
	val effect: StatusEffect,

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
