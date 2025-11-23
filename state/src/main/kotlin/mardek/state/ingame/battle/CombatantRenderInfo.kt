package mardek.state.ingame.battle

import mardek.content.stats.StatusEffect
import mardek.state.util.Rectangle

class CombatantRenderInfo {
	val effectHistory = StatusEffectHistory()

	val lastStatusEffectParticleEmissions = mutableMapOf<StatusEffect, Long>()

	/**
	 * The last point in time (`System.nanoTime()`) where a player pointed to this combatant as the potential target
	 * for an attack, skill, or item. This is only used to determine whether the blue 'target selection blink' should
	 * be rendered, and is not important for the course of the battle.
	 */
	var lastPointedTo = 0L

	/**
	 * When the `AnimationRenderer` encounters the `StrikePoint` node of a combatant, it should set this `strikePoint`
	 * to the position of that node, but only if the combatant is at its base position
	 * (so not e.g. doing a melee attack).
	 *
	 * The `strikePoint` of the attacker and `hitPoint` of the target are needed to determine melee attack positioning.
	 */
	var strikePoint = CombatantRenderPosition.DUMMY

	/**
	 * When the `AnimationRenderer` encounters the `HitPoint` node of a combatant, it should set this `hitPoint`
	 * to the position of that node, but only if the combatant is at its base position
	 * (so not e.g. doing a melee attack).
	 *
	 * The `hitPoint` of the target is needed to determine the attacker position during melee attacks and breath
	 * attacks.
	 */
	var hitPoint = CombatantRenderPosition.DUMMY

	/**
	 * When the `AnimationRenderer` encounters the `BreathSource` node of a combatant, it should set this `breathSource`
	 * to the position of that node, but only if the combatant is at its base position
	 * (so not e.g. doing a melee attack).
	 *
	 * This differs from the `activeBreathSource`, which must always be updated, even if the attacker is **not** at its
	 * base position.
	 *
	 * When a combatant performs a multi-target breath attack, the `idleBreathSource` is used to find the right
	 * attacker position such that its `activeBreathSource` will coincide with the *BreathCentre* of the screen
	 * (typically near the middle).
	 */
	var idleBreathSource = CombatantRenderPosition.DUMMY

	/**
	 * When the `AnimationRenderer` encounters the `BreathSource` node of a combatant, it should set this `breathSource`
	 * to the position of that node, *even if the combatant is moving*.
	 *
	 * This differs from the `idleBreathSource`, which should only be updated while the combatant is at its base
	 * position.
	 *
	 * When a combatant performs a breath attack, the breath particles should spawn at the `activeBreathSource`.
	 */
	var activeBreathSource = CombatantRenderPosition.DUMMY

	/**
	 * When the `AnimationRenderer` encounters the `BreathDistance` node of a combatant, it should set this
	 * `breathDistance`to the position of that node, but only if the combatant is at its base position
	 * (so not e.g. doing a melee attack).
	 *
	 * The `breathDistance` of the attacker and `hitPoint` of the target are needed to determine breath attack
	 * positioning.
	 */
	var breathDistance = CombatantRenderPosition.DUMMY

	/**
	 * Render status effect particles here
	 */
	var statusEffectPoint = CombatantRenderPosition.DUMMY

	val castingParticlePositions = mutableListOf<CombatantRenderPosition>()

	var core = CombatantRenderPosition.DUMMY

	/**
	 * This contains information that is used to render the damage indicator whenever combatants are attacked or
	 * gain/lose health or mana.
	 */
	var lastDamageIndicator: DamageIndicator? = null

	/**
	 * When the turn of this combatant is forcibly skipped (e.g. due to paralysis or numbness + berserk),
	 * `lastForcedTurn` will contain the time at which the turn was skipped, as well as the desired flash/blink
	 * color.
	 *
	 * This information is used by the renderer to show the yellow/red paralysis/numbness blink/flash.
	 */
	var lastForcedTurn: ForcedTurnBlink? = null

	/**
	 * The position where the information block (health, mana, status effects, etc...) of this combatant was rendered
	 * during the last frame, or null when the combatant info was not rendered last frame (e.g. because the first
	 * frame hasn't been rendered yet)
	 */
	var renderedInfoBlock: Rectangle? = null
}

class CombatantRenderPosition(
	/**
	 * The X-coordinate, in pixels
	 */
	val x: Float,

	/**
	 * The Y-coordinate, in pixels
	 */
	val y: Float
) {
	override fun toString() = String.format("(%.1f, %.1f)", x, y)

	companion object {

		/**
		 * Upon instantiating a `CombatantRenderInfo`, all render positions (e.g. `core` and `hitPoint`) are set to
		 * `DUMMY`. When the `AnimationRenderer` sees that e.g. `core == DUMMY`, it knows that it hasn't assigned the
		 * `core` position yet.
		 */
		val DUMMY = CombatantRenderPosition(-123f, -456f)
	}
}
