package mardek.content.skill

import com.github.knokko.bitser.BitEnum

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
