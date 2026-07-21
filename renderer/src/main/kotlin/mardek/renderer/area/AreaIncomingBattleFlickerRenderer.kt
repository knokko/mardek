package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.state.ingame.area.AreaSuspensionIncomingBattle
import kotlin.time.Duration.Companion.milliseconds

internal fun renderAreaIncomingBattleFlicker(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionIncomingBattle) return

		val fade = areaTimings.oscillate(
			0f, 1f, 100.milliseconds,
			referenceTime = suspension.startedFlickerAt
		)
		if (fade > 0.001f) {
			context.addColorBatch(2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0f, 0f, 0f, fade),
			)
		}
	}
}
