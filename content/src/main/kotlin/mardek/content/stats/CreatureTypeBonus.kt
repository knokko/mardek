package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.ReferenceField

/**
 * Bonus damage against a specific [CreatureType]. Weapons and skills can have this.
 */
@BitStruct(backwardCompatible = true)
class CreatureTypeBonus(

	/**
	 * The creature type against which the weapon or skill deals more damage
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "creature types")
	val type: CreatureType,

	/**
	 * How much extra damage will be dealt: `finalDamage = originalDamage * (1f + modifier)`.
	 *
	 * This can be negative, which would cause attacks to deal less damage rather than more damage.
	 */
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.1)
	val modifier: Float,
) {

	@Suppress("unused")
	private constructor() : this(CreatureType(), 0f)

	override fun toString() = "+ $modifier * damage against $type"
}
