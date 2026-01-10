package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.characters.PlayableCharacter
import java.util.UUID

/**
 * Represents an equipment slot of a playable character. This class defines which types of items can be put in this
 * equipment slot.
 */
@BitStruct(backwardCompatible = true)
class EquipmentSlot(
	/**
	 * The unique ID of this equipment slot, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The display name of this equipment slot, which is shown when the player hovers their mouse cursor over this
	 * equipment slot.
	 */
	@BitField(id = 1)
	val displayName: String,

	/**
	 * Only items with one of these `ItemType`s can be put in this slot. Note that non-equippable items (whose with
	 * [Item.equipment] is `null`) can never be equipped, even if their item type is in `itemTypes`.
	 *
	 * When this array is empty, not a single item can be equipped in this slot, which is e.g. used for the off-hand
	 * of characters that cannot equip shields.
	 */
	@BitField(id = 2)
	@ReferenceField(stable = false, label = "item types")
	val itemTypes: Array<ItemType>,

	/**
	 * Whether this equipment slot is allowed to be empty. When this is `false`, the user is not allowed to take the
	 * item from this equipment slot, unless the item is immediately replaced.
	 * By default, this is `false` for all weapon slots, and `true` for all other slots.
	 */
	@BitField(id = 3)
	val canBeEmpty: Boolean,
) {

	@Suppress("unused")
	private constructor() : this(UUID.randomUUID(), "", emptyArray(), false)

	/**
	 * Checks whether the candidate item is allowed to go in this equipment slot.
	 */
	fun isAllowed(candidate: Item, owner: PlayableCharacter) = candidate.equipment != null &&
			itemTypes.contains(candidate.type) &&
			(candidate.equipment.onlyUser == null || owner === candidate.equipment.onlyUser)
}
