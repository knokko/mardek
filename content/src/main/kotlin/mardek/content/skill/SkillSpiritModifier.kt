package mardek.content.skill

import com.github.knokko.bitser.BitEnum

/**
 * The damage of many skills depends on the Spirit of the caster. For almost all skills, the damage scales linearly
 * with the spirit. But, a couple of skills are different. Such skills will have
 * `skill.damage.spiritModifier != SkillSpiritModifier.None`.
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class SkillSpiritModifier {

	/**
	 * The default spirit modifier:
	 * - For melee skills, the spirit is ignored
	 * - For ranged/magic skills, the damage scales linearly with the spirit of the caster
	 */
	None,

	// TODO CHAP3 Figure out these custom formulas
	/**
	 * The skill uses the same formula as Spirit Blade (or the skill is Spirit Blade)
	 */
	SpiritBlade,

	/**
	 * The skill uses the same formula as Divine Glory (or the skill is Divine Glory)
	 */
	DivineGlory,

	/**
	 * The skill uses the same formula as Lay on Hands (or the skill is Lay on Hands)
	 */
	LayOnHands,

	/**
	 * The skill uses the same formula as Green Lightning (or the skill is Green Lightning)
	 */
	GreenLightning,
}
