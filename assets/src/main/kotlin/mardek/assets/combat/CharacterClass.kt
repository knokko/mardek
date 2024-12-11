package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.inventory.ArmorType
import mardek.assets.inventory.WeaponType
import mardek.assets.skill.SkillClass

@BitStruct(backwardCompatible = false)
class CharacterClass(
	@BitField(ordering = 0)
	val rawName: String,

	@BitField(ordering = 1)
	val displayName: String,

	@BitField(ordering = 2)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass,

	@BitField(ordering = 3)
	@ReferenceField(stable = false, label = "weapon types")
	val weaponType: WeaponType,

	@BitField(ordering = 4)
	@ReferenceField(stable = false, label = "armor types")
	val armorTypes: ArrayList<ArmorType>,
) {

	internal constructor() : this("", "", SkillClass(), WeaponType(), ArrayList(0))

	override fun toString() = displayName
}
