package mardek.assets.combat

import com.github.knokko.bitser.BitStruct

@BitStruct(backwardCompatible = false)
class Resistances(
	val elements: ArrayList<ElementalResistance>,
	val effects: ArrayList<EffectResistance>
) {

	constructor() : this(ArrayList(0), ArrayList(0))
}
