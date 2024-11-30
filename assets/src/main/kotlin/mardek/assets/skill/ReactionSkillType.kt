package mardek.assets.skill

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
enum class ReactionSkillType(val isMelee: Boolean, val isOffensive: Boolean) {
	MeleeAttack(true, true),
	RangedAttack(false, true),
	MeleeDefense(true, false),
	RangedDefense(false, false)
}
