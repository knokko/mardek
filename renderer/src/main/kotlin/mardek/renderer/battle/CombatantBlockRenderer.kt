package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.util.ResourceBarRenderer
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.util.Rectangle
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun renderCombatantHealth(
	combatant: CombatantState, healthBar: ResourceBarRenderer,
	currentTime: Long, opacity: Float = 1f
): Int {
	var displayedHealth = combatant.currentHealth
	val lastDamage = combatant.renderInfo.lastDamageIndicator

	fun healthChangeDuration(
		lastDamage: DamageIndicatorHealth
	) = min(2_000_000_000.0, 50_000_000.0 * lastDamage.gainedHealth.absoluteValue)

	if (lastDamage is DamageIndicatorHealth) {
		val changeDuration = healthChangeDuration(lastDamage)
		val progress = (currentTime - lastDamage.time).toDouble() / changeDuration
		if (progress < 1.0) {
			val newHealth = lastDamage.oldHealth + lastDamage.gainedHealth
			displayedHealth = (progress * newHealth + (1.0 - progress) * lastDamage.oldHealth).roundToInt()
			displayedHealth = if (lastDamage.gainedHealth >= 0) min(displayedHealth, combatant.currentHealth)
			else max(displayedHealth, combatant.currentHealth)
		}
	}
	healthBar.renderBar(displayedHealth, combatant.maxHealth, opacity)
	if (lastDamage is DamageIndicatorHealth) {
		val changeDuration = healthChangeDuration(lastDamage).toFloat()
		val fadeDuration = min(2_000_000_000f, 100_000_000f * lastDamage.gainedHealth.absoluteValue)
		val passedTime = currentTime - lastDamage.time
		var lostOpacity = if (passedTime <= changeDuration) 1f
		else if (passedTime <= changeDuration + fadeDuration) 1f - (passedTime - changeDuration) / fadeDuration
		else 0f

		lostOpacity *= opacity
		if (lostOpacity > 0) {
			healthBar.renderLost(
				displayedHealth, lastDamage.oldHealth,
				combatant.maxHealth, lostOpacity
			)
		}
	}
	return displayedHealth
}

internal fun maybeRenderSelectionBlink(state: CombatantState, colorBatch: Vk2dColorBatch, region: Rectangle) {
	if (state.renderInfo.lastPointedTo != 0L) {
		val blinkTime = 500_000_000L
		val passedTime = System.nanoTime() - state.renderInfo.lastPointedTo
		if (passedTime < blinkTime) {
			val intensity = 1f - passedTime.toFloat() / blinkTime.toFloat()
			val blinkColor = rgba(0.1f, 0.1f, 0.9f, 0.5f * intensity)
			colorBatch.gradient(
				region.minX, region.minY, region.maxX, region.maxY,
				blinkColor, 0, blinkColor
			)
		}
	}
}
