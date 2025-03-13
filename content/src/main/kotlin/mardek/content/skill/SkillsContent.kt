package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class SkillsContent(
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "skill classes")
	val classes: ArrayList<SkillClass>,

	@BitField(id = 1)
	val sirenSongs: ArrayList<SirenSong>,

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "skills")
	val reactionSkills: ArrayList<ReactionSkill>,

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "skills")
	val passiveSkills: ArrayList<PassiveSkill>,
) {

	constructor() : this(ArrayList(), ArrayList(), ArrayList(), ArrayList())
}
