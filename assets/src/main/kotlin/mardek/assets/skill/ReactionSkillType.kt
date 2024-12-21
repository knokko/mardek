package mardek.assets.skill

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
enum class ReactionSkillType(val isMelee: Boolean, val isOffensive: Boolean) {
	MeleeAttack(true, true),
	RangedAttack(false, true),
	MeleeDefense(true, false),
	RangedDefense(false, false);

	companion object {
		fun fromString(raw: String) = when (raw) {
			"P_ATK" -> MeleeAttack
			"P_DEF" -> MeleeDefense
			"M_ATK" -> RangedAttack
			"M_DEF" -> RangedDefense
			else -> throw IllegalArgumentException("Unknown skill category $raw")
		}
	}
}
