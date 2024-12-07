package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class SkillAssets(
	@BitField(ordering = 0)
	@ReferenceFieldTarget(label = "skill classes")
	val classes: ArrayList<SkillClass>,

	@BitField(ordering = 1)
	val sirenSongs: ArrayList<SirenSong>,

	@BitField(ordering = 2)
	@ReferenceFieldTarget(label = "reaction skills")
	val reactionSkills: ArrayList<ReactionSkill>,

	@BitField(ordering = 3)
	@ReferenceFieldTarget(label = "passive skills")
	val passiveSkills: ArrayList<PassiveSkill>,
) {

	constructor() : this(ArrayList(), ArrayList(), ArrayList(), ArrayList())
}
