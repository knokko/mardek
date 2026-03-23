package mardek.content.skill

import com.github.knokko.bitser.BitEnum

/**
 * Determines whether a skill can be used in combat, outside combat, or both
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class SkillCombatRequirement {
	/**
	 * Skill can be used both outside combat and during combat
	 */
	Always,

	/**
	 * Skill can only be used during combat
	 */
	InCombat,

	/**
	 * Skill must not be used during combat
	 */
	OutsideCombat,
}
