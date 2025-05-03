package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.BITSER
import java.util.*

@BitStruct(backwardCompatible = true)
class PartyLayout(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	@NestedFieldSetting(path = "c", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val positions: Array<PartyLayoutPosition>,
) {
	@BitField(id = 2)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this("", emptyArray())

	override fun toString() = "Layout($name)"
}

@BitStruct(backwardCompatible = true)
class PartyLayoutPosition(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,
) {
	@Suppress("unused")
	private constructor() : this(0, 0)

	override fun toString() = "($x, $y)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
