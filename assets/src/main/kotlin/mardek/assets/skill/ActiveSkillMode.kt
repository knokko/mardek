package mardek.assets.skill

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.VariableIntOrdinal)
enum class ActiveSkillMode {
	Melee,
	Ranged,
	Self
}
