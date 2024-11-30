package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class SkillAssets(
	@BitField(ordering = 0)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "skill classes")
	val classes: ArrayList<SkillClass>,

	@BitField(ordering = 1)
	@CollectionField
	val sirenSongs: ArrayList<SirenSong>,

	@BitField(ordering = 2)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "reaction skills")
	val reactionSkills: ArrayList<ReactionSkill>,

	@BitField(ordering = 3)
	@CollectionField
	@ReferenceFieldTarget(stable = false, label = "passive skills")
	val passiveSkills: ArrayList<PassiveSkill>,
) {

	constructor() : this(ArrayList(), ArrayList(), ArrayList(), ArrayList())
}
