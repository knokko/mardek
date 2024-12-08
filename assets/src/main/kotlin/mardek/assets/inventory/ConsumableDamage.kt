package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element

@BitStruct(backwardCompatible = false)
class ConsumableDamage(
		@BitField(ordering = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		val power: Int,

		@BitField(ordering = 1)
		@IntegerField(expectUniform = false, minValue = 1)
		val spirit: Int,

		@BitField(ordering = 2)
		@ReferenceField(stable = false, label = "elements")
		val element: Element,
) {

	@Suppress("unused")
	private constructor() : this(0, 0, Element())

	override fun toString() = "Damage($power, $spirit, $element)"
}
