package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.state.util.RenderTiming
import mardek.renderer.util.ResourceBarRenderer
import mardek.state.ingame.battle.CombatantState
import mardek.state.ingame.battle.DamageIndicatorHealth
import mardek.state.util.Rectangle
import mardek.content.util.Time
import mardek.content.util.min
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun renderCombatantHealth(
	combatant: CombatantState, healthBar: ResourceBarRenderer,
	timing: RenderTiming, opacity: Float = 1f
): Int {
	var displayedHealth = combatant.currentHealth
	val lastDamage = combatant.renderInfo.lastDamageIndicator

	fun healthChangeDuration(
		lastDamage: DamageIndicatorHealth
	) = min(2.seconds, 50.milliseconds * lastDamage.gainedHealth.absoluteValue)

	if (lastDamage is DamageIndicatorHealth) {
		val changeDuration = healthChangeDuration(lastDamage)
		displayedHealth = timing.interpolate(
			lastDamage.time, lastDamage.oldHealth,
			changeDuration, lastDamage.oldHealth + lastDamage.gainedHealth, true
		).coerceIn(0 .. combatant.maxHealth)
	}
	healthBar.renderBar(displayedHealth, combatant.maxHealth, opacity)
	if (lastDamage is DamageIndicatorHealth) {
		val changeDuration = healthChangeDuration(lastDamage)
		val fadeDuration = min(2.seconds, 100.milliseconds * lastDamage.gainedHealth.absoluteValue)
		val passedTime = timing.elapsedTimeSince(lastDamage.time)
		var lostOpacity = if (passedTime <= changeDuration) 1f
		else if (passedTime <= changeDuration + fadeDuration) 1f - ((passedTime - changeDuration) / fadeDuration).toFloat()
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

internal fun maybeRenderSelectionBlink(
	timing: RenderTiming, state: CombatantState,
	colorBatch: Vk2dColorBatch, region: Rectangle
) {
	if (state.renderInfo.lastPointedTo != Time.ZERO) {
		val blinkTime = 500.milliseconds
		val passedTime = timing.elapsedTimeSince(state.renderInfo.lastPointedTo)
		if (passedTime < blinkTime) {
			val intensity = 1f - (passedTime / blinkTime).toFloat()
			val blinkColor = rgba(0.1f, 0.1f, 0.9f, 0.5f * intensity)
			colorBatch.gradient(
				region.minX, region.minY, region.maxX, region.maxY,
				blinkColor, 0, blinkColor
			)
		}
	}
}
