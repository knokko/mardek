package mardek.assets.area.objects

class AreaShop(
	val shopName: String,
	val x: Int,
	val y: Int,
	val waresConstantName: String
) {

	override fun toString() = "AreaShop($shopName, x=$x, y=$y, wares=DefaultShops.$waresConstantName)"

	override fun equals(other: Any?) = other is AreaShop && shopName == other.shopName && x == other.x &&
			y == other.y && waresConstantName == other.waresConstantName

	override fun hashCode() = waresConstantName.hashCode()
}
