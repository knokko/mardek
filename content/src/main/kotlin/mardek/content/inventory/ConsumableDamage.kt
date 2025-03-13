package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.combat.Element

@BitStruct(backwardCompatible = true)
class ConsumableDamage(
		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		val power: Int,

		@BitField(id = 1)
		@IntegerField(expectUniform = false, minValue = 1)
		val spirit: Int,

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "elements")
		val element: Element,
) {

	init {
		if (power == 0 || spirit == 0) throw IllegalArgumentException("Invalid damage $this")
	}

	@Suppress("unused")
	private constructor() : this(-1, -1, Element())

	override fun toString() = "Damage($power, $spirit, $element)"
}
