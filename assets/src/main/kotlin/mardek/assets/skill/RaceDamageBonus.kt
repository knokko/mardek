package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.CharacterRace

@BitStruct(backwardCompatible = false)
class RaceDamageBonus(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "races")
	val race: CharacterRace,

	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.1)
	val bonusFraction: Float,
) {

	@Suppress("unused")
	private constructor() : this(CharacterRace(), 0f)

	override fun toString() = "+ $bonusFraction * damage against $race"
}
