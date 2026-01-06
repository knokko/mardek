package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.action.WalkSpeed
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionOpeningDoor
import mardek.state.ingame.area.AreaSuspensionPlayerWalking
import kotlin.math.min
import kotlin.time.Duration

internal fun renderAreaFadeEffects(areaContext: AreaRenderContext) {
	areaContext.apply {
		val currentTime = state.determineCurrentTime()
		val fadeIn = min(1.0, currentTime / AreaState.DOOR_OPEN_DURATION).toFloat()
		var fadeOut = 1f

		when (val suspension = state.suspension) {
			is AreaSuspensionOpeningDoor -> {
				fadeOut = ((suspension.finishTime - currentTime) / AreaState.DOOR_OPEN_DURATION).toFloat()
			}
			is AreaSuspensionActions -> {
				if (suspension.actions.switchAreaAt >= Duration.ZERO) {
					fadeOut = ((suspension.actions.switchAreaAt - currentTime) / AreaState.DOOR_OPEN_DURATION).toFloat()
				}
			}
			is AreaSuspensionPlayerWalking -> {
				if (suspension.destination.transition != null) {
					fadeOut = ((suspension.destination.arrivalTime - currentTime) / WalkSpeed.Normal.duration).toFloat()
				}
			}
			else -> {}
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
