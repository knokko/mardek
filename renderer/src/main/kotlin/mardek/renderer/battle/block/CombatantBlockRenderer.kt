package mardek.renderer.battle.block

import mardek.renderer.ui.ResourceBarRenderer
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun renderCombatantHealth(combatant: CombatantState, healthBar: ResourceBarRenderer, currentTime: Long): Int {
	var displayedHealth = combatant.currentHealth
	val lastDamage = combatant.lastDamageIndicator

	fun fadeProgress(lastDamageTime: Long) = (currentTime - lastDamageTime).toDouble() / 2000_000_000.0

	if (lastDamage is DamageIndicatorHealth) {
		val progress = fadeProgress(lastDamage.time)
		if (progress < 1.0) {
			val newHealth = lastDamage.oldHealth + lastDamage.gainedHealth
			displayedHealth = (progress * newHealth + (1.0 - progress) * lastDamage.oldHealth).roundToInt()
			displayedHealth = if (lastDamage.gainedHealth >= 0) min(displayedHealth, combatant.currentHealth)
			else max(displayedHealth, combatant.currentHealth)
		}
	}
	healthBar.renderBar(displayedHealth, combatant.maxHealth)
	if (lastDamage is DamageIndicatorHealth) {
		val progress = fadeProgress(lastDamage.time)
		val opacity = if (progress < 1) 1.0 else if (progress < 2) 4 * (1.25 - progress) else 0.0
		if (opacity > 0) {
			healthBar.renderLost(
				displayedHealth, lastDamage.oldHealth,
				combatant.maxHealth, opacity.toFloat()
			)
		}
	}
	return displayedHealth
}
