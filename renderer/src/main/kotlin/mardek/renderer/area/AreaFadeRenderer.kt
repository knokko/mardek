package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.action.WalkSpeed
import mardek.state.ingame.area.AreaState
import kotlin.math.min
import kotlin.time.Duration

internal fun renderAreaFadeEffects(areaContext: AreaRenderContext) {
	areaContext.apply {
		val currentTime = state.actions?.currentTime ?: state.currentTime
		val fadeIn = min(1.0, currentTime / AreaState.DOOR_OPEN_DURATION).toFloat()
		var fadeOut = 1f
		state.openingDoor?.run {
			fadeOut = ((this.finishTime - currentTime) / AreaState.DOOR_OPEN_DURATION).toFloat()
		}
		state.actions?.run {
			if (switchAreaAt >= Duration.ZERO) {
				fadeOut = ((switchAreaAt - currentTime) / AreaState.DOOR_OPEN_DURATION).toFloat()
			}
		}
		state.nextPlayerPosition?.run {
			if (transition != null) {
				fadeOut = ((arrivalTime - currentTime) / WalkSpeed.Normal.duration).toFloat()
			}
		}

		val fade = 1f - fadeIn * fadeOut
		if (fade > 0.001f) {
			context.addColorBatch(2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0f, 0f, 0f, fade),
			)
		}
	}
}
