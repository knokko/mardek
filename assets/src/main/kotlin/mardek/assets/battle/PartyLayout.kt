package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.StableReferenceFieldId
import java.util.*

@BitStruct(backwardCompatible = false)
class PartyLayout(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	@NestedFieldSetting(path = "c", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val positions: Array<PartyLayoutPosition>,
) {
	@BitField(ordering = 2)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this("", emptyArray())

	override fun toString() = "Layout($name)"
}

@BitStruct(backwardCompatible = false)
class PartyLayoutPosition(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,
) {
	@Suppress("unused")
	private constructor() : this(0, 0)

	override fun toString() = "($x, $y)"

	override fun equals(other: Any?) = other is PartyLayoutPosition && x == other.x && y == other.y

	override fun hashCode() = x + 127 * y
}
