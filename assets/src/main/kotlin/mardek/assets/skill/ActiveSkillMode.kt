package mardek.assets.skill

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class ActiveSkillMode {
	Melee,
	Ranged,
	Self
}
