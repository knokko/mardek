package mardek.renderer.area.ui

import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.multiplyAlpha
import mardek.content.util.Time
import mardek.renderer.area.AreaRenderContext
import mardek.state.ingame.actions.AreaActionsState
import mardek.state.ingame.area.AreaSuspensionActions
import kotlin.time.Duration

internal fun renderActionFlash(areaContext: AreaRenderContext) {
	areaContext.run {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionActions || suspension.actions.lastFlashTime == Time.ZERO) return

		val passedTime = context.timing.elapsedTimeSince(suspension.actions.lastFlashTime)
		if (passedTime <= Duration.ZERO || passedTime >= AreaActionsState.FLASH_DURATION) return
		val intensity = context.timing.oscillate(
			0f, 1f, AreaActionsState.FLASH_DURATION,
			referenceTime = suspension.actions.lastFlashTime
		)

		var currentColor = multiplyAlpha(suspension.actions.lastFlashColor, 0.8f * intensity)
		if (alpha(currentColor) == 0.toByte()) currentColor = changeAlpha(currentColor, 1)

		val batch = context.addColorBatch(2)
		batch.fill(region.minX, region.minY, region.maxX, region.maxY, currentColor)
	}
}
