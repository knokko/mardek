package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * The section/part of the `Content` that is related to [Skill]s
 */
@BitStruct(backwardCompatible = true)
class SkillsContent(

	/**
	 * All the skill classes that players can have (or technically, all skill classes that
	 * [mardek.content.stats.CharacterClass] can have)
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "skill classes")
	val classes: ArrayList<SkillClass>,

	/**
	 * All the siren songs that can be used by Elwyen
	 */
	@BitField(id = 1)
	val sirenSongs: ArrayList<SirenSong>,

	/**
	 * All the reaction skills
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "skills")
	val reactionSkills: ArrayList<ReactionSkill>,

	/**
	 * All the passive skills
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "skills")
	val passiveSkills: ArrayList<PassiveSkill>,
) {

	constructor() : this(ArrayList(), ArrayList(), ArrayList(), ArrayList())
}
