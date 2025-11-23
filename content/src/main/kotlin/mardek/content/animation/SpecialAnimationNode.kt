package mardek.content.animation

import com.github.knokko.bitser.BitEnum

/**
 * The `AnimationNode` class has a nullable field called `special` of type `SpecialAnimationNode`. When a node has a
 * non-null `special`, it has custom rendering rules that the `AnimationRenderer` will implement.
 *
 * For instance, when `node.special = TargetingCursor`, the node should only be rendered if the player is currently
 * choosing the target of a move, and is pointing to the combatant being rendered.
 */
@BitEnum(mode = BitEnum.Mode.Name)
enum class SpecialAnimationNode(

	/**
	 * When an `AnimationNode` has a `SpecialAnimationNode` with `skipChildren = true`, the children of that node should
	 * **not** be rendered. This is `true` for e.g. `HitPoint` and `StrikePoint`, which are act as marker nodes, and
	 * shouldn't be rendered.
	 */
	val skipChildren: Boolean
) {
	/**
	 * Don't render this. When the `AnimationRenderer` encounters this node, it should set the corresponding
	 * `CombatantRenderInfo.hitPoint` to the node position. The X-coordinate of the target `hitPoint` is needed in the
	 * melee attack positioning logic.
	 */
	HitPoint(true),

	/**
	 * Don't render this. When the `AnimationRenderer` encounters this node, it should set the corresponding
	 * `CombatantRenderInfo.strikePoint` to the node position. The X-coordinate of the attacker `strikePoint` is
	 * needed in the melee attack positioning logic.
	 */
	StrikePoint(true),

	/**
	 * Don't render this. Render status effects at the position of this node.
	 */
	StatusEffectPoint(true),

	/**
	 * Don't render this. When the `AnimationRenderer` encounters this node, it should set the corresponding
	 * `CombatantRenderInfo.idleBreathSource` and `activeBreathSource` to the node position.
	 *
	 * When the attacker does a breath attack, the (fire) breath particles should spawn at `activeBreathSource`.
	 *
	 * Furthermore, when the attacker performs a breath attack against multiple targets, the attacker should be
	 * positioned such that its `activeBreathSource`is at the 'BreathCentre' of the screen, which is usually near the
	 * middle of the screen. The `idleBreathSource` is used to find the right attacker position.
	 */
	BreathSource(true),

	/**
	 * Don't render this. When the `AnimationRenderer` encounters this node, it should set the corresponding
	 * `CombatantRenderInfo.breathDistance` to the node position. The X-coordinate of the attacker `breathDistance` is
	 * needed in the sngle-target breath attack positioning logic.
	 */
	BreathDistance(true),

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
