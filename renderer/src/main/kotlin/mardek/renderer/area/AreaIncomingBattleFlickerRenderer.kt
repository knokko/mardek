package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.state.ingame.area.AreaSuspensionIncomingBattle
import kotlin.math.abs
import kotlin.math.max

internal fun renderAreaIncomingBattleFlicker(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionIncomingBattle) return

		if (suspension.estimatedNanoStartAt == 0L) {
			suspension.estimatedNanoStartAt = System.nanoTime() + (suspension.startAt - state.currentTime).inWholeNanoseconds
		}

		val flickerPeriod = 100_000_000L
		val relativeTime = (max(0L, suspension.estimatedNanoStartAt - System.nanoTime())) % flickerPeriod
		val fade = 2f * abs(flickerPeriod / 2 - relativeTime) / flickerPeriod
		if (fade > 0.001f) {
			context.addColorBatch(2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0f, 0f, 0f, fade),
			)
		}
	}
}
