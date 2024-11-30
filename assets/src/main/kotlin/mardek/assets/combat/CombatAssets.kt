package mardek.assets.combat

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class CombatAssets(
	@BitField(ordering = 0)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "stats")
	val stats: ArrayList<CombatStat>,

	@BitField(ordering = 1)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "elements")
	val elements: ArrayList<Element>,

	@BitField(ordering = 2)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "status effects")
	val statusEffects: ArrayList<StatusEffect>,

	@BitField(ordering = 3)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "races")
	val races: ArrayList<CharacterRace>,

	@BitField(ordering = 4)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "character classes")
	val classes: ArrayList<CharacterClass>,
) {

	constructor() : this(
		ArrayList(0), ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0)
	)
}
