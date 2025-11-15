package mardek.state.title

import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState

/**
 * This state is reached when the player clicks the **Begin** button on the title screen to start a new campaign.
 * During this state, the title screen should gradually fade out, after which the actual campaign starts.
 *
 * The game should stay in this state for approximately `FADE_DURATION` nanoseconds, unless the player presses a key to
 * skip it. Right after transitioning to this state, the title screen should be rendered, but no longer update.
 * However, the rendered title screen should gradually fade out until it is no longer visible.
 * When it is no longer visible, the game will transition to the actual campaign state.
 */
class StartNewGameState(
	/**
	 * The state of the title screen that should still be rendered, but no longer updated.
	 */
	val titleState: TitleScreenState,

	/**
	 * The 'state' of the campaign that should start soon. This 'state' should normally be the starting state of
	 * chapter 1.
	 */
	private val campaignState: CampaignState,

	/**
	 * The player-selected name of the campaign that should start soon
	 */
	private val campaignName: String,
): GameState {

	/**
	 * This should be the time at which the player clicked on the **Begin** button, at least approximately.
	 */
	val beginButtonClickTime = System.nanoTime()

	override fun update(context: GameStateUpdateContext): GameState {
		var shouldStart = System.nanoTime() - beginButtonClickTime > FADE_DURATION
		while (!shouldStart) {
			val event = context.input.consumeEvent() ?: break
			if (event !is InputKeyEvent) continue
			if (!event.didPress) continue

			val skipKeys = arrayOf(InputKey.Interact, InputKey.Cancel, InputKey.Escape, InputKey.ToggleMenu)
			if (skipKeys.contains(event.key)) shouldStart = true
		}

		return if (shouldStart) {
			campaignState.markSessionStart()
			InGameState(campaignState, campaignName)
		} else this
	}

	companion object {
		/**
		 * The default duration of this state.
		 */
		const val FADE_DURATION = 1_000_000_000L
	}
}
