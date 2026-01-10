package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.inventory.EquipmentSlot
import mardek.content.skill.SkillClass

/**
 * Represents an (RPG) class that playable characters can have. Currently, each playable character as a unique
 * `CharacterClass`, but that is not a requirement.
 */
@BitStruct(backwardCompatible = true)
class CharacterClass(

	/**
	 * The raw name of the character class. This is needed during Flash importing, but not very useful otherwise.
	 */
	@BitField(id = 0)
	val rawName: String,

	/**
	 * The display name of the character class, which is sometimes shown in the inventory tab.
	 */
	@BitField(id = 1)
	val displayName: String,

	/**
	 * The skill class defines the skills that characters with this class can learn.
	 */
	@BitField(id = 2)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass,

	/**
	 * The equipment slots that characters with this class have. The length of this array is the number of equipment
	 * slots that the characters have, and each element of this array defines which items can go into that equipment
	 * slot (or no items at all).
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "equipment slots")
	val equipmentSlots: Array<EquipmentSlot>,
) {

	constructor() : this("", "", SkillClass(), emptyArray())

	override fun toString() = displayName
}
