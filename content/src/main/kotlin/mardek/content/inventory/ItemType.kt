package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * Represents a type of item, e.g. WEAPON and MISCELLANEOUS
 */
@BitStruct(backwardCompatible = true)
class ItemType(

	/**
	 * The display name of the item type, as imported from Flash. It is displayed in the inventory UI.
	 */
	@BitField(id = 0)
	val flashName: String,

	/**
	 * Whether the item can *stack*: whether the `amount` can be larger than 1 for an `ItemStack` with an item of this
	 * type. In vanilla, miscellaneous items can be stacked, whereas equippable items cannot be stacked.
	 */
	@BitField(id = 1)
	val canStack: Boolean
) {

	internal constructor() : this("", false)

	override fun toString() = flashName
}
