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

	var strikePoint = CombatantRenderPosition(0f, 0f)

	var hitPoint = CombatantRenderPosition(0f, 0f)

	var statusEffectPoint = CombatantRenderPosition(0f, 0f)

	var core = CombatantRenderPosition(0f, 0f)

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
}
