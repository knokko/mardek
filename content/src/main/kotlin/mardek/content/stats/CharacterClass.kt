package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.ArmorType
import mardek.content.inventory.WeaponType
import mardek.content.skill.SkillClass

@BitStruct(backwardCompatible = true)
class CharacterClass(
	@BitField(id = 0)
	val rawName: String,

	@BitField(id = 1)
	val displayName: String,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass,

	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "weapon types")
	val weaponType: WeaponType?,

	@BitField(id = 4)
	@ReferenceField(stable = false, label = "armor types")
	val armorTypes: ArrayList<ArmorType>,
) {

	constructor() : this("", "", SkillClass(), WeaponType(), ArrayList(0))

	override fun toString() = displayName
}
