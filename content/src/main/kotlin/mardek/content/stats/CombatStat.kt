package mardek.content.stats

import com.github.knokko.bitser.BitEnum

/**
 * Tke fixed combat stats/fields of the game. Each stat has a fixed meaning.
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class CombatStat(val flashName: String) {

	/**
	 * The strength (STR) influences the damage dealt by melee attacks. Melee attack damage usually scales linearly with
	 * the strength of the attacker.
	 */
	Strength("STR"),

	/**
	 * The vitality (VIT) can influence the maximum HP, although most non-player combatants ignore it.
	 */
	Vitality("VIT"),

	/**
	 * The spirit (SPR) influences the damage dealt by ranged/magic attacks. Ranged attack damage usually scales
	 * linearly with the spirit of the attacker.
	 *
	 * Furthermore, the spirit can influence the maximum HP, although most non-player combatants ignore it.
	 */
	Spirit("SPR"),

	/**
	 * The agility determines when each character gets their first turn in combat: the combatant with the highest
	 * agility goes first, the combatant with the second-highest agility goes second, etc...
	 */
	Agility("AGL"),

	/**
	 * The attack (ATK) influences the damage dealt by melee attacks. Melee attack damage usually scales linearly with
	 * `attacker.ATK - target.DEF`.
	 */
	Attack("ATK"),

	/**
	 * The melee defense (DEF) influences the damage received by melee attacks. Melee attack damage usually scales
	 * linearly with `attacker.ATK - target.DEF`.
	 */
	MeleeDefense("DEF"),

	/**
	 * The magic/ranged defense (MDEF) influences the damage received by magic/ranked attacks. Magic attack damage
	 * usually scales linearly with `attacker.SPR - target.MDEF`.
	 */
	RangedDefense("MDEF"),

	/**
	 * The evasion (EVA) influences the chance to get hit by  melee attacks. When the evasion is e.g. 30, there is
	 * a base 70% that a melee attack would hit the combatant.
	 */
	Evasion("EVA"),

	/**
	 * The value of the MaxHealth (hp) is added to the maximum health of the combatant
	 */
	MaxHealth("hp"),

	/**
	 * The value of MaxMana (mp) is added to the maximum mana of the combatant
	 */
	MaxMana("mp")
}
