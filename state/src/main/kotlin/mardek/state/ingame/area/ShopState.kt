package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import mardek.content.area.AreaShop

/**
 * Represents the state of a shop.
 *
 * Currently, this is just the inventory of the shop, but it might be expanded later.
 */
@BitStruct(backwardCompatible = true)
class ShopState(shop: AreaShop) {

	/**
	 * The current inventory of the shop. This changes whenever the player sells an item, or buys an item with a
	 * finite supply.
	 *
	 * The unlimited-supply items are *not* in this inventory: they can be found in [AreaShop.fixedItems].
	 */
	@BitField(id = 0)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 60, maxValue = 60))
	val inventory = shop.initialInventory.copyOf()

	@Suppress("unused")
	private constructor() : this(AreaShop())
}
