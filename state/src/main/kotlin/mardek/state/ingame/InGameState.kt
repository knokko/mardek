package mardek.state.ingame

import mardek.content.Content
import mardek.content.audio.AudioContent
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.ingame.menu.ShownState
import mardek.state.saves.SaveFile
import mardek.state.title.GameOverState
import mardek.state.title.TitleScreenState
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

	/**
	 * When the player presses the Escape button, the game will ask whether the player wants to exit the game.
	 * The player can *confirm* by pressing Escape again, or *cancel* by pressing Q.
	 *
	 * This field is used to track whether this confirmation is in progress,
	 * and will be non-null if and  only if that is the case.
	 */
	var considerExit: ConsiderCampaignExit? = null

	override fun update(context: GameStateUpdateContext): GameState {
		campaign.time += context.timeStep
		campaign.clampHealthAndMana()
		menu.shown = menu.shown.update(campaign.time)

		if (menu.shown is ShownState.FullyShown) {
			menu.update(context.input, context.soundQueue, context.content)
		}
		if (menu.shown is ShownState.FullyHidden) {
			val considerExit = this.considerExit
			if (considerExit == null) {
				campaign.update(CampaignState.UpdateContext(context, campaignName))
				if (campaign.shouldOpenMenu) {
					menu.shown = ShownState.FadingIn(campaign.time)
					menu.refreshCurrentTab(context.content)
					campaign.shouldOpenMenu = false
				}
				if (campaign.gameOver) return GameOverState()
				if (campaign.considerExit) {
					this.considerExit = ConsiderCampaignExit(campaign.time)
					campaign.considerExit = false
				}
			} else {
				val timeSinceConsider = campaign.time.virtualOffset(considerExit.consideredAt)
				if (timeSinceConsider >= ConsiderCampaignExit.FADE_IN) {
					if (considerExit.cancelledAt != null) {
						val timeSinceCancel = campaign.time.virtualOffset(considerExit.cancelledAt!!)
						if (timeSinceCancel >= ConsiderCampaignExit.CANCEL_FADE_OUT) this.considerExit = null
					} else if (considerExit.confirmedAt != null) {
						val timeSinceConfirm = campaign.time.virtualOffset(considerExit.confirmedAt!!)
						if (timeSinceConfirm >= ConsiderCampaignExit.EXIT_FADE_OUT) {
							context.saves.createSave(
								context.content, campaign,
								campaignName, SaveFile.Type.Auto,
							)
							return TitleScreenState()
						}
					}
				}

				val listenToKeys = considerExit.cancelledAt == null && considerExit.confirmedAt == null &&
						timeSinceConsider >= ConsiderCampaignExit.FADE_IN

				while (true) {
					val nextEvent = context.input.consumeEvent() ?: break
					if (nextEvent !is InputKeyEvent || !nextEvent.didPress || !listenToKeys) continue
					if (nextEvent.key == InputKey.Escape) considerExit.confirmedAt = campaign.time
					if (nextEvent.key == InputKey.Cancel) considerExit.cancelledAt = campaign.time
				}
			}
		}
		return this
	}

	override fun determineMusic(content: Content?, audioContent: AudioContent) = if (content == null) {
		MusicPlayerJob(null)
	} else MusicPlayerJob(campaign.determineMusicTrack(content))
}
