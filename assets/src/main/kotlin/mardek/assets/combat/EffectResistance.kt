package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class EffectResistance(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "status effects")
	val effect: StatusEffect,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val percentage: Int,
) {

	@Suppress("unused")
	private constructor() : this(StatusEffect(), 0)

	override fun equals(other: Any?) = other is EffectResistance && effect === other.effect && percentage == other.percentage

	override fun hashCode() = effect.hashCode() - 13 * percentage
}
