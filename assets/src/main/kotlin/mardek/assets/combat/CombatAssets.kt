package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class CombatAssets(
	@BitField(ordering = 0)
	@ReferenceFieldTarget(label = "stats")
	val stats: ArrayList<CombatStat>,

	@BitField(ordering = 1)
	@ReferenceFieldTarget(label = "elements")
	val elements: ArrayList<Element>,

	@BitField(ordering = 2)
	@ReferenceFieldTarget(label = "status effects")
	val statusEffects: ArrayList<StatusEffect>,

	@BitField(ordering = 3)
	@ReferenceFieldTarget(label = "creature types")
	val races: ArrayList<CreatureType>,

	@BitField(ordering = 4)
	@ReferenceFieldTarget(label = "character classes")
	val classes: ArrayList<CharacterClass>,
) {

	constructor() : this(
		ArrayList(0), ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0)
	)
}
