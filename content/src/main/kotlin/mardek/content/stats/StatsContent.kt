package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class StatsContent(
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "elements")
	val elements: ArrayList<Element>,

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "status effects")
	val statusEffects: ArrayList<StatusEffect>,

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "creature types")
	val creatureTypes: ArrayList<CreatureType>,

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "character classes")
	val classes: ArrayList<CharacterClass>,
) {

	constructor() : this(
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0)
	)
}
