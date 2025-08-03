package mardek.state.ingame.battle

import mardek.content.stats.Element

sealed class DamageIndicator(
	val oldHealth: Int,
	val oldMana: Int,
) {
	val time = System.nanoTime()
}

class DamageIndicatorMiss(oldHealth: Int, oldMana: Int) : DamageIndicator(oldHealth, oldMana)

class DamageIndicatorHealth(
	oldHealth: Int,
	oldMana: Int,

	/**
	 * The amount of health that was gained. It will be positive when the combatant was healed, and it will be negative
	 * when the combatant took damage.
	 */
	val gainedHealth: Int,
	val element: Element,

	/**
	 * When non-zero, this overrides the 'blink' color
	 */
	val overrideColor: Int,
) : DamageIndicator(oldHealth, oldMana)

class DamageIndicatorMana(
	oldHealth: Int,
	oldMana: Int,

	/**
	 * The amount of mana that was gained. It will be positive when the combatant gained mana, and it will be negative
	 * when the combatant lost mana.
	 */
	val gainedMana: Int,
	val element: Element,

	/**
	 * When non-zero, this overrides the 'blink' color
	 */
	val overrideColor: Int,
) : DamageIndicator(oldHealth, oldMana)
