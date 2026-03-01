package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.area.AreaShop

/**
 * Tracks the states of all the shops/traders
 */
@BitStruct(backwardCompatible = true)
class ShopsStates {

	@BitField(id = 0, readsMethodResult = true)
	private val mapping = HashMap<AreaShop, ShopState>()

	@BitField(id = 0)
	@Suppress("unused")
	@NestedFieldSetting(path = "k", fieldName = "MAPPING_KEY_PROPERTIES")
	private fun saveMapping() = HashMap(mapping.filter {
		!it.key.initialInventory.contentEquals(it.value.inventory)
	})

	/**
	 * Gets the state of `shop`.
	 *
	 * This will initialize the state of `shop` if it doesn't have a state yet.
	 */
	fun get(shop: AreaShop) = mapping.computeIfAbsent(shop, ::ShopState)

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "shops")
		private const val MAPPING_KEY_PROPERTIES = false
	}
}
