package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.action.WalkSpeed
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionOpeningDoor
import mardek.state.ingame.area.AreaSuspensionPlayerWalking
import mardek.state.ingame.area.loot.BattleLoot
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

internal fun renderAreaFadeEffects(areaContext: AreaRenderContext) {
	areaContext.apply {
		val fadeIn = min(1.0, state.currentTime / AreaState.DOOR_OPEN_DURATION).toFloat()
		val postBattleFadeIn = min(
			1.0, (state.currentTime - state.finishedBattleAt) / BattleLoot.FADE_OUT_DURATION.nanoseconds
		).toFloat()
		var fadeOut = 1f

		when (val suspension = state.suspension) {
			is AreaSuspensionOpeningDoor -> {
				fadeOut = ((suspension.finishTime - state.currentTime) / AreaState.DOOR_OPEN_DURATION).toFloat()
			}
			is AreaSuspensionActions -> {
				if (suspension.actions.switchAreaAt >= Duration.ZERO) {
					fadeOut = ((suspension.actions.switchAreaAt - state.currentTime) / AreaState.DOOR_OPEN_DURATION).toFloat()
				}
			}
			is AreaSuspensionPlayerWalking -> {
				if (suspension.destination.transition != null) {
					fadeOut = ((suspension.destination.arrivalTime - state.currentTime) / WalkSpeed.Normal.duration).toFloat()
				}
			}
			else -> {}
		}

		val fade = 1f - fadeIn * postBattleFadeIn * fadeOut
		if (fade > 0.001f) {
			context.addColorBatch(2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0f, 0f, 0f, fade),
			)
		}
	}
}
