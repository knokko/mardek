package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

/**
 * Represents a type of item, e.g. `WEAPON: GREATSWORD` and `MISCELLANEOUS`
 */
@BitStruct(backwardCompatible = true)
class ItemType(

	/**
	 * The display name of the item type, as imported from Flash. It is displayed in the inventory UI.
	 */
	@BitField(id = 0)
	val displayName: String,

	/**
	 * The color that represents this item in the minified inventory grid that is shown below each character in the
	 * chest/battle loot screen. (For instance, weapons are red, armor is green, and accessories are orange.)
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val gridColor: Int,
) {

	constructor() : this("", 0)

	override fun toString() = displayName
}
