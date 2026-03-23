package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * The resistances against elements and status effects of a combatant, skill, or piece of equipment
 */
@BitStruct(backwardCompatible = true)
class Resistances(

	/**
	 * The resistances against each element. When no resistance against an element is specified, the resistance is 0%
	 * (so no additional damage and no damage reduction).
	 */
	@BitField(id = 0)
	val elements: ArrayList<ElementalResistance>,

	/**
	 * The resistances against each status effect. When no resistance against a status effect is specified, the
	 * resistance is 0% (nothing).
	 */
	@BitField(id = 1)
	val effects: ArrayList<EffectResistance>
) {

	constructor() : this(ArrayList(0), ArrayList(0))

	/**
	 * Gets the resistance against [element]. This simply sums up the `modifier` of each resistance against [element].
	 *
	 * The damage formula is basically: `finalDamage = (1 - resistance) * originalDamage`
	 */
	fun get(element: Element): Float = elements.sumOf {
		if (it.element === element) it.modifier.toDouble() else 0.0
	}.toFloat()

	/**
	 * Gets the resistance against [effect] (percentage). When a combatant would normally get a status effect, this is
	 * the chance that the combatant 'evades' it.
	 */
	fun get(effect: StatusEffect): Int = effects.sumOf {
		if (it.effect === effect) it.percentage else 0
	}
}
