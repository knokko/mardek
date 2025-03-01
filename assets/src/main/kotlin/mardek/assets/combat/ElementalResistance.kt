package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class ElementalResistance(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.05)
	val modifier: Float
) {

	@Suppress("unused")
	private constructor() : this(Element(), 0f)

	override fun equals(other: Any?) = other is ElementalResistance &&
			this.element === other.element && this.modifier == other.modifier

	override fun hashCode() = element.hashCode() + modifier.hashCode()
}
