package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

/**
 * A resistance against a specific [StatusEffect]
 */
@BitStruct(backwardCompatible = true)
class EffectResistance(

	/**
	 * The status effect that is resisted
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "status effects")
	val effect: StatusEffect,

	/**
	 * The resistance percentage (e.g. 0 does nothing, 100 means complete immunity to [effect])
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100, commonValues = [100])
	val percentage: Int,
) {

	@Suppress("unused")
	private constructor() : this(StatusEffect(), 0)

	override fun equals(other: Any?) = other is EffectResistance && effect === other.effect && percentage == other.percentage

	override fun hashCode() = effect.hashCode() - 13 * percentage

	override fun toString() = "$percentage% $effect"
}
