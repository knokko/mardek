package mardek.state.ingame

import mardek.content.Content
import mardek.content.audio.AudioContent
import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.ingame.menu.ShownState
import mardek.state.title.GameOverState
import mardek.state.util.MusicPlayerJob

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
		campaign.time += context.timeStep
		campaign.clampHealthAndMana()
		menu.shown = menu.shown.update(campaign.time)

		if (menu.shown is ShownState.FullyShown) {
			menu.update(context.input, context.soundQueue, context.content)
		}
		if (menu.shown is ShownState.FullyHidden) {
			campaign.update(CampaignState.UpdateContext(context, campaignName))
			if (campaign.shouldOpenMenu) {
				menu.shown = ShownState.FadingIn(campaign.time)
				menu.refreshCurrentTab(context.content)
				campaign.shouldOpenMenu = false
			}
			if (campaign.gameOver) return GameOverState()
		}
		return this
	}

	override fun determineMusic(content: Content?, audioContent: AudioContent) = if (content == null) {
		MusicPlayerJob(null)
	} else MusicPlayerJob(campaign.determineMusicTrack(content))
}
