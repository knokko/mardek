package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class AreaShop(

	@BitField(ordering = 0)
	val shopName: String,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 3)
	val waresConstantName: String
) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, "")

	override fun toString() = "AreaShop($shopName, x=$x, y=$y, wares=DefaultShops.$waresConstantName)"

	override fun equals(other: Any?) = other is AreaShop && shopName == other.shopName && x == other.x &&
			y == other.y && waresConstantName == other.waresConstantName

	override fun hashCode() = waresConstantName.hashCode()
}
