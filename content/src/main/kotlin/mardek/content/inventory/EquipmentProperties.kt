package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.*
import mardek.content.skill.Skill

/**
 * The properties that only equippable items (weapons, shields, etc...) have.
 */
@BitStruct(backwardCompatible = true)
class EquipmentProperties(

	/**
	 * The skills that players can toggle while this piece item is equipped, *even when these skills are not mastered
	 * yet*.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "skills")
	val skills: ArrayList<Skill>,

	/**
	 * The stats that will be increased while this item is equipped. Weapons typically increase the ATK stat, whereas
	 * armor pieces typically increase the DEF and MDEF stat.
	 */
	@BitField(id = 1)
	val stats: ArrayList<StatModifier>,

	/**
	 * The elemental damage bonuses that are added while this item is equipped. For instance, a fire damage bonus
	 * would empower fire-elemental attacks of whoever equips the item.
	 */
	@BitField(id = 2)
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	/**
	 * The elemental & status effect resistances that are added while this item is equipped.
	 */
	@BitField(id = 3)
	val resistances: Resistances,

	/**
	 * The status effects that are automatically given to whomever equips this item, and cannot be removed until this
	 * item is unequipped. This is used by items like Yellow Fairy and the Cursed Blade.
	 */
	@BitField(id = 4)
	@ReferenceField(stable = false, label = "status effects")
	val autoEffects: ArrayList<StatusEffect>,

	/**
	 * This field must be non-null if and only if this item is a weapon. If so, this field describes the weapon-only
	 * properties of this item.
	 */
	@BitField(id = 5, optional = true)
	val weapon: WeaponProperties?,

	/**
	 * This field must be non-null if and only if this item is an armor piece. If so, field specifies the armor type
	 * of this item, which determines which players can carry the armor, and in which equipment slot.
	 */
	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "armor types")
	val armorType: ArmorType?,

	/**
	 * This field must be non-null if and only if this item is a gemstone. If so, it specifies the gemsplosion power &
	 * element of the item.
	 */
	@BitField(id = 7, optional = true)
	val gem: GemProperties?,

	/**
	 * When this field is non-null, this item can only be equipped by the player with this name. This is used for e.g.
	 * Righteous Glory, which can only be equipped by Vehrn.
	 */
	@BitField(id = 8, optional = true)
	val onlyUser: String?, // TODO CHAP2 turn into reference

	/**
	 * The chance of having the same effect as the charismatic performance skill. TODO CHAP3 work this out
	 */
	@BitField(id = 9)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val charismaticPerformanceChance: Int,
) {

	@Suppress("unused")
	private constructor() : this(
		ArrayList(0), ArrayList(0), ArrayList(0), Resistances(), ArrayList(0),
		null, null, null, null, 0
	)

	/**
	 * The equipment slot in which this item can be carried.
	 */
	fun getSlotType(): EquipmentSlotType {
		if (weapon != null) return EquipmentSlotType.MainHand
		if (armorType == null) return EquipmentSlotType.Accessory
		return armorType.slot
	}

	/**
	 * If someone were to equip this item, this method compute by how much the given `stat` of that person would be
	 * increased. This is mostly used in the UI, e.g. to predict how much the ATK of a player would be increased by
	 * equipping this weapon.
	 */
	fun getStat(stat: CombatStat): Int {
		var result = 0
		for (modifier in stats) {
			if (modifier.stat == stat) result += modifier.adder
		}
		return result
	}
}
