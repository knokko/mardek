package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class Resistances(
	@BitField(id = 0)
	val elements: ArrayList<ElementalResistance>,

	@BitField(id = 1)
	val effects: ArrayList<EffectResistance>
) {

	constructor() : this(ArrayList(0), ArrayList(0))

	fun get(element: Element): Float = elements.sumOf {
		if (it.element === element) it.modifier.toDouble() else 0.0
	}.toFloat()

	fun get(effect: StatusEffect): Int = effects.sumOf {
		if (it.effect === effect) it.percentage else 0
	}
}
