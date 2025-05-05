package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.state.ingame.battle.CombatantState
import mardek.state.title.AbsoluteRectangle

fun maybeRenderSelectionBlink(state: CombatantState, uiRenderer: UiRenderer, region: AbsoluteRectangle) {
	if (state.lastPointedTo != 0L) {
		val blinkTime = 500_000_000L
		val passedTime = System.nanoTime() - state.lastPointedTo
		if (passedTime < blinkTime) {
			val intensity = 1f - passedTime.toFloat() / blinkTime.toFloat()
			uiRenderer.beginBatch()
			val blinkColor = rgba(0.1f, 0.1f, 0.9f, 0.5f * intensity)
			uiRenderer.fillColor(region.minX, region.minY, region.maxX, region.maxY, 0, Gradient(
				0, 0, region.width, region.height, blinkColor, 0, blinkColor
			)
			)
			uiRenderer.endBatch()
		}
	}
}
