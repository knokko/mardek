package mardek.content.skill

import com.github.knokko.bitser.BitEnum

/**
 * The 4 types of [ReactionSkill]s. The "Skills" tab in the in-game menu has 1 section for each `ReactionSkillType`,
 * as well as a section for [ActiveSkill]s and a section for [PassiveSkill]s.
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class ReactionSkillType(val isMelee: Boolean, val isOffensive: Boolean) {

	/**
	 * Reaction skills with this type are applied when the player is performing a melee attack
	 */
	MeleeAttack(true, true),

	/**
	 * Reaction skills with this type are applied when the player is casting a magic (ranged) skill
	 */
	RangedAttack(false, true),

	/**
	 * Reaction skills with this type are applied when the player is the target of a melee attack
	 */
	MeleeDefense(true, false),

	/**
	 * Reaction skills with this type are applied when the player is the target of a ranged/magic attack
	 */
	RangedDefense(false, false);

	companion object {

		/**
		 * This method should only be used for importing from the Flash skills list
		 */
		fun fromString(raw: String) = when (raw) {
			"P_ATK" -> MeleeAttack
			"P_DEF" -> MeleeDefense
			"M_ATK" -> RangedAttack
			"M_DEF" -> RangedDefense
			else -> throw IllegalArgumentException("Unknown skill category $raw")
		}
	}
}
