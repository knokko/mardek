package mardek.state.ingame

import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.title.GameOverState

/**
 * When the state is an `InGameState`, the player is currently playing the game, and not e.g. in the title screen.
 */
class InGameState(
	/**
	 * The state of the game/campaign
	 */
	val campaign: CampaignState,

	/**
	 * The name of the campaign being played, which is used to determine the save folder location.
	 */
	val campaignName: String,
): GameState {

	/**
	 * The state of the in-game menu, which tracks e.g. whether the player is currently inside the inventory.
	 */
	val menu = InGameMenuState(campaign)

	override fun update(context: GameStateUpdateContext): GameState {
		campaign.totalTime += context.timeStep
		campaign.clampHealthAndMana()
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
