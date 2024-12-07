package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.PossibleStatusEffect
import mardek.assets.combat.StatModifier
import mardek.assets.combat.StatusEffect
import mardek.assets.skill.ElementalDamageBonus
import mardek.assets.skill.Skill

@BitStruct(backwardCompatible = false)
class EquipmentProperties(

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "skills")
	val skills: ArrayList<Skill>,

	@BitField(ordering = 1)
	val stats: ArrayList<StatModifier>,

	@BitField(ordering = 2)
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	@BitField(ordering = 3)
	val elementalResistances: ArrayList<ElementalDamageBonus>,

	@BitField(ordering = 4)
	val statusResistances: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 5)
	@ReferenceField(stable = false, label = "status effects")
	val autoEffects: ArrayList<StatusEffect>,

	@BitField(ordering = 6, optional = true)
	val weapon: WeaponProperties?,

	@BitField(ordering = 7, optional = true)
	val armor: ArmorProperties?,

	@BitField(ordering = 8, optional = true)
	val gem: GemProperties?,

	@BitField(ordering = 9, optional = true)
	val onlyUser: String?, // TODO Use playable character reference
) {

	@Suppress("unused")
	private constructor() : this(
			ArrayList(0), ArrayList(0), ArrayList(0),
			ArrayList(0), ArrayList(0), ArrayList(0),
			null, null, null, null
	)
}
