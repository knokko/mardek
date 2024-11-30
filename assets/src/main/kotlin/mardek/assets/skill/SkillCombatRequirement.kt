package mardek.assets.skill

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.VariableIntOrdinal)
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
