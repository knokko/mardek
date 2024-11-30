package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.CharacterRace

@BitStruct(backwardCompatible = false)
class WeaponRaceBonus(

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "races")
	val race: CharacterRace,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false)
	val factor: Int
) {

	@Suppress("unused")
	private constructor() : this(CharacterRace(), 0)
}
