package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

/**
 * Represents a damage bonus (or damage loss) against combatants of a specific element.
 */
@BitStruct(backwardCompatible = true)
class ElementalDamageBonus(

	/**
	 * The bonus works against combatants of this element
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	/**
	 * The damage modifier: basically `finalDamage = (1 + modifier) * originalDamage`
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05)
	val modifier: Float,
) {

	@Suppress("unused")
	private constructor() : this(Element(), 0f)

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
