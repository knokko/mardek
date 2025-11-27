package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * Represents a type of armor: each piece of armor has an `ArmorType`. Each playable character has a list of
 * `ArmorType`s, and can only equip armor pieces with one of those types.
 */
@BitStruct(backwardCompatible = true)
class ArmorType(

	/**
	 * The key/id of the armor type, as imported from Flash (e.g. "ArS" or "Sh"). It only serves a purpose during
	 * importing.
	 */
	@BitField(id = 0)
	val key: String,

	/**
	 * The name of the armor type (uppercase), which is displayed in the inventory UI.
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * The equipment slot in which characters can equip this type of armor.
	 */
	@BitField(id = 2)
	val slot: EquipmentSlotType,
) {

	@Suppress("unused")
	private constructor() : this("", "", EquipmentSlotType.Body)

	override fun toString() = "$name ($key)"
}
