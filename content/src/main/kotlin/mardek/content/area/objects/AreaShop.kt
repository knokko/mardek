package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * Represents a shop in an area. The player can buy or sell items in a shop. The player can open the shop by
 * interacting with it.
 */
@BitStruct(backwardCompatible = true)
class AreaShop(

	/**
	 * The name of the shop, as imported from Flash. I don't know yet what this does.
	 */
	@BitField(id = 0)
	val shopName: String,

	x: Int,
	y: Int,

	/**
	 * This field is imported from Flash, and determines which items the player can buy in this shop.
	 */
	@BitField(id = 1)
	val waresConstantName: String
) : StaticAreaObject(x, y) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, "")

	override fun toString() = "AreaShop($shopName, x=$x, y=$y, wares=DefaultShops.$waresConstantName)"
}
