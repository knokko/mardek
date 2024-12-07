package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.PossibleStatusEffect
import mardek.assets.skill.RaceDamageBonus

@BitStruct(backwardCompatible = false)
class WeaponProperties(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "weapon types")
	val type: WeaponType,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val critChance: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val hitChance: Int,

	@BitField(ordering = 3)
	@FloatField(expectMultipleOf = 0.1)
	val hpDrain: Float,

	@BitField(ordering = 4)
	val raceBonuses: ArrayList<RaceDamageBonus>,

	@BitField(ordering = 5)
	val addEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 6, optional = true)
	val hitSound: String?, // TODO Turn into reference
) {

	@Suppress("unused")
	private constructor() : this(
			WeaponType(), 0, 0, 0f,
			ArrayList(0), ArrayList(0), ""
	)
}
