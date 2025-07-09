package mardek.renderer.area

internal fun collectIncomingBattleIndicator(areaContext: AreaRenderContext) {
	areaContext.apply {
		val incomingRandomBattle = state.incomingRandomBattle
		if (incomingRandomBattle != null) {
			val playerPosition = state.getPlayerPosition(0)
			renderJobs.add(SpriteRenderJob(
				x = tileSize * playerPosition.x,
				y = tileSize * (playerPosition.y - 1) - 4 * scale,
				sprite = if (incomingRandomBattle.canAvoid) {
					context.content.ui.blueAlertBalloon
				} else context.content.ui.redAlertBalloon
			))
		}
	}
}
