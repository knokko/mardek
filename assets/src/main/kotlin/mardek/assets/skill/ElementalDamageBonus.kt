package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element

@BitStruct(backwardCompatible = false)
class ElementalDamageBonus(
	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.05)
	val modifier: Float,
) {

	@Suppress("unused")
	private constructor() : this(Element(), 0f)
}
