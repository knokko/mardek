package mardek.state.ingame.menu

import mardek.content.util.Time
import kotlin.time.Duration.Companion.milliseconds

/**
 * This is the type of [InGameMenuState.shown], which is used to track whether the in-game menu is currently open,
 * and whether it is currently fading in or fading out.
 */
sealed class ShownState {

	/**
	 * Updates this state, and returns the next state (which is usually this state).
	 *
	 * Note that all subclasses are immutable, so this method won't change any internal state.
	 */
	abstract fun update(campaignTime: Time): ShownState

	/**
	 * This state indicates that the in-game menu is completely visible/shown
	 */
	class FullyShown : ShownState() {

		override fun update(campaignTime: Time) = this
	}

	/**
	 * This state indicates that the in-game menu is currently fading out:
	 * - it started fading out at [since]
	 * - it will be faded out completely at `since + FADE_DURATION`
	 */
	class FadingOut(

		/**
		 * The campaign time at which the in-game menu started the fade-out effect.
		 */
		val since: Time
	) : ShownState() {

		override fun update(campaignTime: Time): ShownState {
			return if (campaignTime.virtualOffset(since) >= FADE_DURATION) FullyHidden() else this
		}
	}

	/**
	 * This state indicates that the in-game menu is completely hidden (so the game is active/not paused).
	 */
	class FullyHidden : ShownState() {

		override fun update(campaignTime: Time) = this
	}

	/**
	 * This state indicates that the in-game menu is currently fading in:
	 * - it started fading in at [since]
	 * - it will be completely visible at `since + FADE_DURATION`
	 */
	class FadingIn(

		/**
		 * The campaign time at which the in-game menu started the fade-in effect
		 */
		val since: Time
	) : ShownState() {

		override fun update(campaignTime: Time): ShownState {
			return if (campaignTime.virtualOffset(since) >= FADE_DURATION) FullyShown() else this
		}
	}

	companion object {

		/**
		 * The duration of the in-game menu fade-in and fade-out effect
		 */
		val FADE_DURATION = 250.milliseconds
	}
}
