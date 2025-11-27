package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.Element

/**
 * For consumable items that deal damage (e.g. Liquid Lightning), this class describes their damage and element.
 */
@BitStruct(backwardCompatible = true)
class ConsumableDamage(

	/**
	 * The power of the consumable. The damage is usually proportional to `power - magicDefense`.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val power: Int,

	/**
	 * The 'spirit' of the consumable. This will be used in the damage formula, instead of the spirit of the thrower.
	 * The damage is usually proportional to the spirit.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val spirit: Int,

	/**
	 * The element of the damage that the consumable will deal (e.g. Air for Liquid Lightning).
	 */
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
