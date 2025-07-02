package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.multiplyAlpha
import mardek.state.ingame.actions.AreaActionsState
import kotlin.math.abs
import kotlin.math.pow

internal fun renderActionFlash(areaContext: AreaRenderContext) {
	areaContext.run {
		val actions = state.actions ?: return
		val passedTime = System.nanoTime() - actions.lastFlashTime
		if (passedTime <= 0L || passedTime >= AreaActionsState.FLASH_DURATION) return

		val relativeTime = passedTime.toDouble() / AreaActionsState.FLASH_DURATION.toDouble()
		val intensity = 1f - 2f * abs(0.5f - relativeTime.toFloat()).pow(1f)

		var currentColor = multiplyAlpha(actions.lastFlashColor, 0.8f * intensity)
		if (alpha(currentColor) == 0.toByte()) currentColor = changeAlpha(currentColor, 1)

		val batch = context.addColorBatch(2)
		batch.fill(region.minX, region.minY, region.maxX, region.maxY, currentColor)
	}
}
