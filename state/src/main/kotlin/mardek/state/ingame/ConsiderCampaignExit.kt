package mardek.state.ingame

import mardek.content.util.Time
import kotlin.time.Duration.Companion.milliseconds

/**
 * This class is the type of [InGameState.considerExit]: when a player presses Escape,
 * this class will check whether the player confirms, and track the potential fade-in and fade-out.
 */
class ConsiderCampaignExit(

	/**
	 * The value of [CampaignState.time] when the player pressed Escape, and opened the exit confirmation modal
	 */
	val consideredAt: Time,
) {

	/**
	 * This field is initially `null`, but becomes non-null when the player presses Q (and cancels the exit modal).
	 * When non-null, it is the value of [CampaignState.time] when the player pressed Q.
	 *
	 * This field is needed to render the fade-out of the modal.
	 */
	var cancelledAt: Time? = null

	/**
	 * This field is initially `null`, but becomes non-null when the player presses Escape *again*
	 * (and thus *confirms* the exit).
	 * When non-null, it is the value of [CampaignState.time] when the player pressed Escape for the second time.
	 *
	 * This field is needed to render the fade-out of the modal.
	 */
	var confirmedAt: Time? = null

	companion object {

		/**
		 * The fade-in duration of this confirmation modal
		 */
		val FADE_IN = 100.milliseconds

		/**
		 * The fade-out duration of this confirmation modal, **if** the player presses Q
		 */
		val CANCEL_FADE_OUT = 250.milliseconds

		/**
		 * The fade-out duration of this confirmation modal, **if** the player presses Escape again
		 */
		val EXIT_FADE_OUT = 500.milliseconds
	}
}
