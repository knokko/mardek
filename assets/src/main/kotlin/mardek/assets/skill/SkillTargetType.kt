package mardek.assets.skill

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.VariableIntOrdinal)
enum class SkillTargetType {
	Self,
	Single,
	AllEnemies,
	AllAllies,
	Any
}
