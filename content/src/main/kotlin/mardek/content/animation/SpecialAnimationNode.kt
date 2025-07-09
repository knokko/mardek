package mardek.content.animation

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Name)
enum class SpecialAnimationNode {
	/**
	 * Don't render this. Something to do with the positioning of the strike animation?
	 */
	HitPoint,

	/**
	 * Don't render this. Something to do with the positioning of the strike animation?
	 */
	StrikePoint,

	/**
	 * Don't render this. Render status effects here?
	 */
	StatusEffectPoint,

	/**
	 * This animation node should only be rendered when this combatant is being selected in targeting mode
	 */
	TargetingCursor,

	/**
	 * This animation node should only be rendered when this player is on turn, and still choosing its next move
	 */
	OnTurnCursor,

	/**
	 * The middle/core of this combatant. Don't render this.
	 */
	Core,

	/**
	 * Don't render this. I'm not sure what its purpose is though...
	 */
	Exclaim,

	/**
	 * The node that should be rendered while the combatant is doing a melee attack. The sprite should be chosen
	 * based on the element of the attack.
	 */
	ElementalSwing,

	/**
	 * When a combatant casts a magic skill, elemental particles should be spawned at this node to indicate that a spell
	 * is being cast.
	 */
	ElementalCastingSparkle,

	/**
	 * The circle node that should be rendered while the combatant is casting a magic skill. The sprite should be chosen
	 * based on the element of the skill.
	 */
	ElementalCastingCircle,

	/**
	 * The aura node that should be rendered while the combatant is casting a magic skill. The sprite should be chosen
	 * based on the element of the skill.
	 */
	ElementalCastingBackground,

	/**
	 * This node will render a weapon, so the skin should be chosen based on the name of the equipped weapon.
	 */
	Weapon,

	/**
	 * This node will render a shield, so the skin should be chosen based on the name of the equipped shield.
	 */
	Shield,
}
