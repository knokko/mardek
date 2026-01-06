package mardek.renderer.area

import mardek.state.ingame.area.AreaSuspensionIncomingRandomBattle

internal fun collectIncomingBattleIndicator(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
		if (suspension is AreaSuspensionIncomingRandomBattle) {
			val playerPosition = state.getPlayerPosition(0)
			renderJobs.add(SpriteRenderJob(
				x = tileSize * playerPosition.x,
				y = tileSize * (playerPosition.y - 1) - 4 * scale,
				sprite = if (suspension.canAvoid) {
					context.content.ui.blueAlertBalloon
				} else context.content.ui.redAlertBalloon
			))
		}
	}
}
