package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.state.ingame.area.AreaSuspensionIncomingBattle
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal fun renderAreaIncomingBattleFlicker(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionIncomingBattle) return

		val fadeAlpha = areaTimings.oscillate(
			0f, 255f, 100.milliseconds,
			referenceTime = suspension.startedFlickerAt
		).roundToInt()
		if (fadeAlpha > 0) {
			context.addColorBatch(2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0, 0, 0, fadeAlpha),
			)
		}
	}
}
