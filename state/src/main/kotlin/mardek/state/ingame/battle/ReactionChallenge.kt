package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.skill.ReactionSkillType
import mardek.content.util.Time
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
	val primaryType: ReactionSkillType,

	/**
	 * The campaign time when the reaction started
	 */
	@BitField(id = 1)
	val startTime: Time,
) {

	/**
	 * - When the player hasn't pressed the Interact key yet since the start of this reaction challenge,
	 * this field will be [NOT_YET_REACTED], which means that the reaction challenge is pending.
	 * If this field is still [NOT_YET_REACTED] when `campaignTime >= startTime + MAX_CLICK_AFTER`,
	 * the player loses the challenge due to inactivity.
	 *
	 * - When the player presses the Interact key for the first time after the start of this reaction challenge,
	 * this field will be set to `campaignTime - startTime`. The player wins the challenge if this is between
	 * [MIN_CLICK_AFTER] and [MAX_CLICK_AFTER]. Pressing the Interact key more than once has no effect; the first
	 * press counts.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false)
	var clickedAfter = NOT_YET_REACTED
		private set

	@Suppress("unused")
	private constructor() : this(ReactionSkillType.MeleeDefense, Time.ZERO)

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
		clickedAfter = Duration.ZERO
	}

	/**
	 * Return true if the challenge is pending: when the outcome of the reaction challenge is not yet known.
	 * The challenge is pending when:
	 * - the player has not clicked yet, and
	 * - less than `MAX_CLICK_AFTER` time has passed since the start of the challenge
	 */
	fun isPending(currentTime: Time) = clickedAfter == NOT_YET_REACTED &&
			currentTime.virtualOffset(startTime) <= MAX_CLICK_AFTER &&
			currentTime.virtual >= startTime.virtual

	/**
	 * This method should be called right after the player pressed E.
	 */
	fun click(currentTime: Time) {
		if (clickedAfter == NOT_YET_REACTED) clickedAfter = currentTime.virtualOffset(startTime)
	}

	companion object {

		val NOT_YET_REACTED = (-1).seconds

		/**
		 * The duration of the reaction challenge:
		 * - The caret/cursor starts at the left window border when `campaignTime == startTime`
		 * - The caret/cursor reaches the right window border when `campaignTime == startTime + DURATION`
		 */
		val DURATION = 1040.milliseconds

		/**
		 * Once the player presses the Interact key (and [clickedAfter] is set), the reaction bar will 'glow' red or
		 * green (depending on whether the player won the challenge). This red or green 'glow' will start fading
		 * immediately, and it takes `RESULT_FADE_DURATION` until it is gone completely.
		 */
		val RESULT_FADE_DURATION = 1.seconds

		/**
		 * The reaction bar will start fading out [RESULT_FADE_DURATION] after it was finished.
		 * This fade-out takes `FINAL_FADE_DURATION`.
		 */
		val FINAL_FADE_DURATION = 500.milliseconds

		/**
		 * To win the challenge, the player must press the Interact key at least `MIN_CLICK_AFTER` after [startTime],
		 * and at most [MAX_CLICK_AFTER] after [startTime].
		 */
		val MIN_CLICK_AFTER = 580.milliseconds

		/**
		 * To win the challenge, the player must press the Interact key at least [MIN_CLICK_AFTER] after [startTime],
		 * and at most `MAX_CLICK_AFTER` nanoseconds after [startTime].
		 */
		val MAX_CLICK_AFTER = 715.milliseconds
	}
}
