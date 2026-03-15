package mardek.state.ingame.battle

/**
 * An indicator that a player got a level-up during a battle
 */
class LevelUpIndicator(

	/**
	 * The result of `System.nanoTime()` at the time of the level-up
	 */
	val startTime: Long,

	/**
	 * The new level of the combatant
	 */
	val newLevel: Int,
) {

	companion object {

		/**
		 * The duration of the initial 'jump' of the "Level Up!" indicator, in nanoseconds
		 */
		const val JUMP_DURATION = 250_000_000L

		/**
		 * The 'stable' duration of the "Level Up!" indicator, in nanoseconds: this is the time between the end of the
		 * initial 'jump', and the beginning of the fade-out.
		 */
		const val STABLE_DURATION = 2_000_000_000L

		/**
		 * The duration of the 'fade out' of the "Level Up!" indicator, in nanoseconds
		 */
		const val FADE_DURATION = 500_000_000L

		/**
		 * The total duration during which the "Level Up!" indicator is visible, in nanoseconds
		 */
		const val TOTAL_DURATION = JUMP_DURATION + STABLE_DURATION + FADE_DURATION
	}
}
