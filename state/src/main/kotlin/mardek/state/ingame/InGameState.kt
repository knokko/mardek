package mardek.state.ingame

import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.title.GameOverState

class InGameState(val campaign: CampaignState): GameState {

	val menu = InGameMenuState(campaign)

	override fun update(context: GameStateUpdateContext): GameState {
		if (menu.shown) {
			menu.update(context.input, context.soundQueue, context.content)
		} else {
			campaign.update(context)
			if (campaign.shouldOpenMenu) {
				menu.shown = true
				campaign.shouldOpenMenu = false
			}
			if (campaign.gameOver) return GameOverState()
		}
		return this
	}
}
