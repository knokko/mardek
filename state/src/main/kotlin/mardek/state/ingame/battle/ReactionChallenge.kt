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
	@BitField(id = 0)
	val primaryType: ReactionSkillType
) {
	val startTime = System.nanoTime()

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

		const val DURATION = 1040_000_000L
		const val RESULT_FADE_DURATION = 1000_000_000L
		const val FINAL_FADE_DURATION = 500_000_000L
		const val MIN_CLICK_AFTER = 580_000_000L
		const val MAX_CLICK_AFTER = 715_000_000L
	}
}
