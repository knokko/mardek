package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.skill.ReactionSkillType

/**
 * Playable characters can have `ReactionSkill`s, which can give e.g. damage bonuses/reductions. When a playable
 * character is involved in an attack (can be either attacker or target), a reaction challenge will be presented on the
 * screen.
 *
 * To pass this reaction challenge, the player needs to press E at the right time: between `MIN_CLICK_AFTER` and
 * `MAX_CLICK_AFTER` nanoseconds after the start of the challenge. When the challenge is passed, the reaction skills
 * will be applied. Otherwise, they will be ignored.
 */
@BitStruct(backwardCompatible = true)
class ReactionChallenge(

	/**
	 * The 'primary' reaction skill type. This is shown on the left of the reaction challenge UI, but doesn't serve any
	 * special purpose.
	 */
	@BitField(id = 0)
	val primaryType: ReactionSkillType
) {

	/**
	 * The result of `System.nanoTime()` when the reaction challenge started
	 */
	val startTime = System.nanoTime()

	/**
	 * - When the player hasn't pressed the Interact key yet since the start of this reaction challenge,
	 * this field will be `-1`, which means that the reaction challenge is pending. If this field is still -1
	 * when `System.nanoTime() >= startTime + MAX_CLICK_AFTER`, the player loses the challenge due to inactivity.
	 *
	 * - When the player presses the Interact key for the first time after the start of this reaction challenge,
	 * this field will be set to `System.nanoTime() - startTime`. The player wins the challenge if this is between
	 * [MIN_CLICK_AFTER] and [MAX_CLICK_AFTER]. Pressing the Interact key more than once has no effect; the first
	 * press counts.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = -1, maxValue = DURATION)
	var clickedAfter = -1L
		private set

	@Suppress("unused")
	private constructor() : this(ReactionSkillType.MeleeDefense)

	/**
	 * Returns true if and only if the player passed the reaction challenge. Note that this will always return false
	 * while `isPending()` is true.
	 */
	fun wasPassed() = clickedAfter in MIN_CLICK_AFTER..MAX_CLICK_AFTER

	/**
	 * Marks this reaction challenge as 'passed': subsequent calls to [wasPassed] will return `true`. This method
	 * should only be used during unit tests.
	 */
	fun forciblyPass() {
		clickedAfter = MIN_CLICK_AFTER
	}

	/**
	 * Marks this reaction challenge as 'failed': subsequent calls to [wasPassed] and [isPending]
	 * will return `false`. This method should only be used during unit tests.
	 */
	fun forciblyFail() {
		clickedAfter = 1L
	}

	/**
	 * Return true if the challenge is pending: when the outcome of the reaction challenge is not yet known. The
	 * challenge is pending when:
	 * - the player has not clicked yet, and
	 * - fewer than `MAX_CLICK_AFTER` nanoseconds have passed since the start of the challenge
	 */
	fun isPending() = clickedAfter == -1L && System.nanoTime() - startTime <= MAX_CLICK_AFTER

	/**
	 * This method should be called right after the player pressed E.
	 */
	fun click() {
		if (clickedAfter == -1L) clickedAfter = System.nanoTime() - startTime
	}

	companion object {

		/**
		 * The duration of the challenge, in nanoseconds.
		 * - The caret/cursor starts at the left window border when `System.nanoTime() == startTime`
		 * - The caret/cursor reaches the right window border when `System.nanoTime() == startTime + DURATION`
		 */
		const val DURATION = 1040_000_000L

		/**
		 * Once the player presses the Interact key (and [clickedAfter] is set), the reaction bar will 'glow' red or
		 * green (depending on whether the player won the challenge). This red or green 'glow' will start fading
		 * immediately, and it takes `RESULT_FADE_DURATION` nanoseconds until it is gone completely.
		 */
		const val RESULT_FADE_DURATION = 1000_000_000L

		/**
		 * The reaction bar will start fading out [RESULT_FADE_DURATION] nanoseconds after it was finished. This
		 * fade-out takes `FINAL_FADE_DURATION` nanoseconds.
		 */
		const val FINAL_FADE_DURATION = 500_000_000L

		/**
		 * To win the challenge, the player must press the Interact key at least `MIN_CLICK_AFTER` nanoseconds after
		 * [startTime], and at most [MAX_CLICK_AFTER] nanoseconds after [startTime].
		 */
		const val MIN_CLICK_AFTER = 580_000_000L

		/**
		 * To win the challenge, the player must press the Interact key at least [MIN_CLICK_AFTER] nanoseconds after
		 * [startTime], and at most `MAX_CLICK_AFTER` nanoseconds after [startTime].
		 */
		const val MAX_CLICK_AFTER = 715_000_000L
	}
}
