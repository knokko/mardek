package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class AreaShop(

	@BitField(id = 0)
	val shopName: String,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(id = 3)
	val waresConstantName: String
) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, "")

	override fun toString() = "AreaShop($shopName, x=$x, y=$y, wares=DefaultShops.$waresConstantName)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
