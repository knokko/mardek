package mardek.state.ingame.battle

import mardek.content.stats.Element

/**
 * An indicator that shows how much health or many a combatant gains or loses due to a move or attack. The renderer
 * makes sure that the most recent indicator is displayed on the screen, typically by showing the icon of the
 * move/attack element, and a text showing the amount of health or many.
 */
sealed class DamageIndicator(

	/**
	 * The amount of health that the combatant had prior to the move/attack
	 */
	val oldHealth: Int,

	/**
	 * The amount of mana that the combatant had prior to the move/attack
	 */
	val oldMana: Int,
) {

	/**
	 * The time (`System.nanoTime()`) when this indicator was created/spawned
	 */
	val time = System.nanoTime()
}

class DamageIndicatorMiss(oldHealth: Int, oldMana: Int) : DamageIndicator(oldHealth, oldMana)

/**
 * An indicator that shows how much health a combatant gained or last due to a move or attack
 */
class DamageIndicatorHealth(
	oldHealth: Int,
	oldMana: Int,

	/**
	 * The amount of health that was gained. It will be positive when the combatant was healed, and it will be negative
	 * when the combatant took damage.
	 */
	val gainedHealth: Int,

	/**
	 * The element of the attack, whose icon is shown in the damage indicator
	 */
	val element: Element,

	/**
	 * When non-zero, this overrides the 'blink' color
	 */
	val overrideColor: Int,
) : DamageIndicator(oldHealth, oldMana)

/**
 * An indicator that shows how much mana a combatant gained or last due to a move or attack
 */
class DamageIndicatorMana(
	oldHealth: Int,
	oldMana: Int,

	/**
	 * The amount of mana that was gained. It will be positive when the combatant gained mana, and it will be negative
	 * when the combatant lost mana.
	 */
	val gainedMana: Int,

	/**
	 * The element of the attack/move, whose icon is shown in the damage indicator
	 */
	val element: Element,

	/**
	 * When non-zero, this overrides the 'blink' color
	 */
	val overrideColor: Int,
) : DamageIndicator(oldHealth, oldMana)
