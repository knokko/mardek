package mardek.content.skill

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class SkillTargetType {
	Self,
	Single,
	AllEnemies,
	AllAllies,
	Any
}
