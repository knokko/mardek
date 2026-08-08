package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.action.ActionToArea
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.ingame.area.AreaSuspensionOpeningDoor
import mardek.state.ingame.area.AreaSuspensionTransition
import mardek.state.ingame.area.loot.BattleLoot

internal fun renderAreaFadeEffects(areaContext: AreaRenderContext) {
	areaContext.apply {
		val fadeIn = areaTimings.interpolate(
			state.zeroTime.virtualAdd(AreaState.FADE_IN_DELAY), 0f,
			AreaState.FADE_IN_DURATION, 1f, true
		)

		val finishedBattleAt = state.finishedBattleAt
		val postBattleFadeIn = if (finishedBattleAt != null) areaTimings.interpolate(
			finishedBattleAt, 0f,
			BattleLoot.FADE_OUT_DURATION, 1f, true,
		) else 1f
		var fadeOut = 1f

		when (val suspension = state.suspension) {
			is AreaSuspensionOpeningDoor -> {
				fadeOut = areaTimings.interpolate(
					suspension.startTime, 1f,
					AreaSuspensionOpeningDoor.FADE_OUT_DURATION, 0f, true,
				)
			}
			is AreaSuspensionActions -> {
				if (suspension.actions.startAreaSwitch != null) {
					fadeOut = areaTimings.interpolate(
						suspension.actions.startAreaSwitch!!, 1f,
						ActionToArea.FADE_OUT_DURATION, 0f, true,
					)
				}
			}
			is AreaSuspensionTransition -> {
				fadeOut = areaTimings.interpolate(
					suspension.startTime, 1f,
					AreaSuspensionTransition.FADE_DURATION, 0f, true,
				)
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
