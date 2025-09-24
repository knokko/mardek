package mardek.content.animation

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Name)
enum class SpecialAnimationNode(
	val skipChildren: Boolean
) {
	/**
	 * Don't render this. Something to do with the positioning of the strike animation?
	 */
	HitPoint(true),

	/**
	 * Don't render this. Something to do with the positioning of the strike animation?
	 */
	StrikePoint(true),

	/**
	 * Don't render this. Render status effects here?
	 */
	StatusEffectPoint(true),

	/**
	 * This animation node should only be rendered when this combatant is being selected in targeting mode
	 */
	TargetingCursor(false),

	/**
	 * This animation node should only be rendered when this player is on turn, and still choosing its next move
	 */
	OnTurnCursor(false),

	/**
	 * The middle/core of this combatant. Don't render this.
	 */
	Core(true),

	/**
	 * Don't render this. I'm not sure what its purpose is though...
	 */
	Exclaim(true),

	/**
	 * The node that should be rendered while the combatant is doing a melee attack. The sprite should be chosen
	 * based on the element of the attack.
	 */
	ElementalSwing(true),

	/**
	 * When a combatant casts a magic skill, elemental particles should be spawned at this node to indicate that a spell
	 * is being cast.
	 */
	ElementalCastingSparkle(true),

	/**
	 * The circle node that should be rendered while the combatant is casting a magic skill. The sprite should be chosen
	 * based on the element of the skill.
	 */
	ElementalCastingCircle(true),

	/**
	 * The aura node that should be rendered while the combatant is casting a magic skill. The sprite should be chosen
	 * based on the element of the skill.
	 */
	ElementalCastingBackground(true),

	/**
	 * This node will render a weapon, so the skin should be chosen based on the name of the equipped weapon.
	 */
	Weapon(false),

	/**
	 * This node will render a shield, so the skin should be chosen based on the name of the equipped shield.
	 */
	Shield(false),

	/**
	 * Use the facial expression ("norm", "grin", etc...) to select the skin
	 */
	PortraitExpressions(false),

	/**
	 * Use `PortraitInfo.faceSkin` to select the skin
	 */
	PortraitFace(false),

	/**
	 * Use `PortraitInfo.hairSkin` to select the skin
	 */
	PortraitHair(false),

	/**
	 * Use `PortraitInfo.eyeSkin` to select the skin
	 */
	PortraitEye(false),

	/**
	 * Use `PortraitInfo.eyeBrowSkin` to select the skin
	 */
	PortraitEyeBrow(false),

	/**
	 * Use `PortraitInfo.mouthSkin` to select the skin
	 */
	PortraitMouth(false),

	/**
	 * Use `PortraitInfo.ethnicitySkin` to select the skin
	 */
	PortraitEthnicity(false),

	/**
	 * Use `PortraitInfo.armorSkin` to select the skin
	 */
	PortraitArmor(false),

	/**
	 * Use `PortraitInfo.robeSkin` to select the skin
	 */
	PortraitRobe(false),
}
