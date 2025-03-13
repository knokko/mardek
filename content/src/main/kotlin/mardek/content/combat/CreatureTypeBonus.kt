package mardek.content.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class CreatureTypeBonus(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "creature types")
	val type: CreatureType,

	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.1)
	val bonusFraction: Float,
) {

	@Suppress("unused")
	private constructor() : this(CreatureType(), 0f)

	override fun toString() = "+ $bonusFraction * damage against $type"
}
