package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER

/**
 * This is the parent class of the unmovable area object types (e.g. doors & shops). It's primary purpose is some
 * minor code reuse.
 */
@BitStruct(backwardCompatible = true)
abstract class StaticAreaObject(
	/**
	 * The X-coordinate of the tile where this object is placed
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, digitSize = 2)
	val x: Int,

	/**
	 * The Y-coordinate of the tile where this object is placed
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, digitSize = 2)
	val y: Int,
) {
	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
