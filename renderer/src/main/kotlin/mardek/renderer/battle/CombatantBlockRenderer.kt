package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.state.util.RenderTiming
import mardek.renderer.util.ResourceBarRenderer
import mardek.state.ingame.battle.combatant.CombatantState
import mardek.state.util.Rectangle
import mardek.content.util.Time
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal fun renderCombatantHealth(
	combatant: CombatantState, healthBar: ResourceBarRenderer,
	timing: RenderTiming, opacity: Float = 1f
): Int {
	val health = combatant.renderInfo.healthHistory.get(
		combatant.currentHealth, timing
	)
	healthBar.renderBar(health.displayedValue, combatant.maxHealth, opacity)

	val redBar = health.bar
	if (redBar != null) {
		healthBar.renderLost(
			redBar.minValue, redBar.maxValue,
			combatant.maxHealth, (redBar.alpha * opacity).roundToInt(),
		)
	}
	return health.displayedValue
}

internal fun renderCombatantMana(
	combatant: CombatantState, manaBar: ResourceBarRenderer,
	timing: RenderTiming, opacity: Float = 1f
): Int {
	val mana = combatant.renderInfo.manaHistory.get(
		combatant.currentMana, timing
	)
	manaBar.renderBar(mana.displayedValue, combatant.maxMana, opacity)
	return mana.displayedValue
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
