package mardek.state.ingame.battle

/**
 * This class helps [PlayerCombatantState] and the renderer to show how much EXP the player recently gained.
 *
 * When a player gains e.g. 100 XP, a yellow indicator with the text "+100 XP" will be show above the combatant
 * block of the player. This class basically tells the renderer what to show in that text indicator.
 */
class ExperienceIndicators {

	/**
	 * The 'queued' experience that will be put in the next [currentEntry].
	 * When `queuedAmount > 0`, all the queued experience will be included in the next `currentEntry`, as soon as the
	 * existing `currentEntry` becomes `null`.
	 */
	internal var queuedAmount = 0

	private var currentEntry: Entry? = null

	/**
	 * Determines the current EXP gain indicator that should be rendered above the player combatant block of this
	 * player. This method ensures that all gained EXP is eventually shown, possibly by combining them.
	 */
	fun getEntryToDisplay(currentTime: Long): Entry? {
		val previousEntry = currentEntry
		if (previousEntry != null) {
			val passedTime = currentTime - previousEntry.startTime
			if (passedTime < TOTAL_DURATION) return previousEntry
			else currentEntry = null
		}

		if (queuedAmount > 0) {
			currentEntry = Entry(currentTime, queuedAmount)
			queuedAmount = 0
		}

		return currentEntry
	}

	/**
	 * Represents a single EXP gain indicator
	 */
	class Entry(
		/**
		 * The value of `System.nanoTime()` when this indicator was rendered for the first time
		 */
		val startTime: Long,

		/**
		 * The amount of gained EXP to be rendered
		 */
		val amount: Int,
	) {
		override fun equals(other: Any?) = other is Entry && this.startTime == other.startTime &&
				this.amount == other.amount

		override fun hashCode() = startTime.toInt() - 13 * amount
	}

	companion object {

		/**
		 * The amount of time (in nanoseconds) that the text indicator should spend 'jumping', before going to the
		 * 'stable' state
		 */
		const val JUMP_DURATION = 250_000_000L

		/**
		 * The amount of time (in nanoseconds) that the text indicator should stay in the 'stable' state, before
		 * starting to fade out
		 */
		const val STABLE_DURATION = 1_000_000_000L

		/**
		 * The amount of time (in nanoseconds) that the text indicator needs to fade out after the stable state is over
		 */
		const val FADE_DURATION = 250_000_000L

		/**
		 * The amount of time (in nanoseconds) that the text indicator is visible
		 */
		const val TOTAL_DURATION = JUMP_DURATION + STABLE_DURATION + FADE_DURATION
	}
}
