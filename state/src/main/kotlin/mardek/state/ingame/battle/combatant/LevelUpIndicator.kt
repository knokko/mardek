package mardek.state.ingame.battle.combatant

import mardek.content.util.Time
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * An indicator that a player got a level-up during a battle
 */
class LevelUpIndicator(

	/**
	 * The time at which the player achieved the level-up
	 */
	val startTime: Time,

	/**
	 * The new level of the combatant
	 */
	val newLevel: Int,
) {

	companion object {

		/**
		 * The duration of the initial 'jump' of the "Level Up!" indicator
		 */
		val JUMP_DURATION = 250.milliseconds

		/**
		 * The 'stable' duration of the "Level Up!" indicator: this is the time between the end of the
		 * initial 'jump', and the beginning of the fade-out.
		 */
		val STABLE_DURATION = 2.seconds

		/**
		 * The duration of the 'fade out' of the "Level Up!" indicator
		 */
		val FADE_DURATION = 500.milliseconds

		/**
		 * The total duration during which the "Level Up!" indicator is visible
		 */
		val TOTAL_DURATION = JUMP_DURATION + STABLE_DURATION + FADE_DURATION
	}
}