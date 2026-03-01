package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.area.objects.StaticAreaObject
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import java.util.UUID

/**
 * Represents a shop in an area. The player can buy or sell items in a shop. The player can open the shop by
 * interacting with it.
 */
@BitStruct(backwardCompatible = true)
class AreaShop(

	/**
	 * The unique ID of this shop, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The name of the shop, as imported from Flash. This is only used for debugging and editing.
	 */
	@BitField(id = 1)
	val name: String,

	/**
	 * This field determines from which slots the player can buy an endless amount of items.
	 *
	 * When `fixedItems[index] != null`, the player can buy an endless amount of the item at `fixedItems[index]`
	 * (as long as the player has enough money). This is often used in consumable shops, where the player can buy e.g.
	 * an endless amount of potions and antidotes.
	 *
	 * When `fixedItems[index] == null`, the [initialInventory] determines which item the player can buy from the
	 * corresponding slot, and how many instances of that item. Once that item is sold out, the slot acts like an
	 * empty item slot, and can be used to sell items.
	 */
	@BitField(id = 2)
	@NestedFieldSetting(path = "c", optional = true)
	@ReferenceField(stable = false, label = "items")
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 60, maxValue = 60))
	val fixedItems: Array<Item?>,

	/**
	 * The initial inventory of the shop.
	 *
	 * Each slot with index `i` such that `fixedItems[i] == null` will be set to `initialInventory[i]` when the player
	 * opens the shop for the first time.
	 */
	@BitField(id = 3)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 60, maxValue = 60))
	val initialInventory: Array<ItemStack?>,
) {

	constructor() : this(
		UUID(0, 0), "",
		emptyArray(), emptyArray()
	)

	override fun toString() = "AreaShop($name)"
}
