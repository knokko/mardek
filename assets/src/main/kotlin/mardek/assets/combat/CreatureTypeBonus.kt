package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class CreatureTypeBonus(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "races")
	val type: CreatureType,

	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.1)
	val bonusFraction: Float,
) {

	@Suppress("unused")
	private constructor() : this(CreatureType(), 0f)

	override fun toString() = "+ $bonusFraction * damage against $type"
}
