package mardek.renderer.area

import mardek.state.ingame.area.AreaSuspensionIncomingRandomBattle
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal fun collectIncomingBattleIndicator(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
		if (suspension is AreaSuspensionIncomingRandomBattle) {
			val playerPosition = state.getPlayerPosition(0)
			val sprite = if (suspension.canAvoid) {
				context.content.ui.blueAlertBalloon
			} else context.content.ui.redAlertBalloon

			val relativeTime = areaTimings.elapsedTimeSince(suspension.encounteredAt)
			val jumpDuration = 400.milliseconds

			val baseY = (tileSize * (playerPosition.y - 1) - 3 * scale).toFloat()
			val peakY = baseY - 8f * scale
			val floatY = if (relativeTime < jumpDuration) areaTimings.oscillate(
				baseY, peakY, 400.milliseconds, referenceTime = suspension.encounteredAt
			) else baseY

			renderJobs.add(SpriteRenderJob(
				x = tileSize * playerPosition.x,
				y = floatY.roundToInt(),
				sprite = sprite
			))
		}
	}
}
