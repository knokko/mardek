package mardek.content.skill

import com.github.knokko.bitser.BitEnum

/**
 * Determines on which targets a skill can be used, and whether it can be used on multiple targets at the same time.
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class SkillTargetType {

	/**
	 * The caster can only cast this skill on itself
	 */
	Self,

	/**
	 * The caster can cast this skill on any combatant, but only on one combatant at the same time
	 */
	Single,

	/**
	 * The caster can only cast this skill on all enemies at the same time
	 */
	AllEnemies,

	/**
	 * The caster can only cast this skill on all its allies (including itself) at the same time
	 */
	AllAllies,

	/**
	 * The caster can cast this skill on any combatant, or on all allies at the same time, or on all enemies at the
	 * same time. Note that most skills cost twice as much mana, and deal only half damage, when used against multiple
	 * targets at the same time.
	 */
	Any
}
