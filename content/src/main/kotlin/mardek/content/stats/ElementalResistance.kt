package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class ElementalResistance(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05, commonValues=[0.5, -1.0, 1.0, 2.0])
	val modifier: Float
) {

	@Suppress("unused")
	private constructor() : this(Element(), 0f)

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	override fun toString() = "${element.properName} $modifier"
}
