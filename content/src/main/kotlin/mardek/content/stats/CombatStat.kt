package mardek.content.stats

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class CombatStat(val flashName: String) {
	Strength("STR"),
	Vitality("VIT"),
	Spirit("SPR"),
	Agility("AGL"),

	Attack("ATK"),
	MeleeDefense("DEF"),
	RangedDefense("MDEF"),
	Evasion("EVA"),

	MaxHealth("hp"),
	MaxMana("mp")
}
