package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.*
import mardek.content.skill.Skill

@BitStruct(backwardCompatible = true)
class EquipmentProperties(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "skills")
	val skills: ArrayList<Skill>,

	@BitField(id = 1)
	val stats: ArrayList<StatModifier>,

	@BitField(id = 2)
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	@BitField(id = 3)
	val resistances: Resistances,

	@BitField(id = 4)
	@ReferenceField(stable = false, label = "status effects")
	val autoEffects: ArrayList<StatusEffect>,

	@BitField(id = 5, optional = true)
	val weapon: WeaponProperties?,

	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "armor types")
	val armorType: ArmorType?,

	@BitField(id = 7, optional = true)
	val gem: GemProperties?,

	@BitField(id = 8, optional = true)
	val onlyUser: String?,

	@BitField(id = 9)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val charismaticPerformanceChance: Int,
) {

	@Suppress("unused")
	private constructor() : this(
		ArrayList(0), ArrayList(0), ArrayList(0), Resistances(), ArrayList(0),
		null, null, null, null, 0
	)

	fun getSlotType(): EquipmentSlotType {
		if (weapon != null) return EquipmentSlotType.MainHand
		if (armorType == null) return EquipmentSlotType.Accessory
		return armorType.slot
	}
}
