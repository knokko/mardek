package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.skill.ReactionSkillType

@BitStruct(backwardCompatible = true)
class ReactionChallenge(
	@BitField(id = 0)
	val primaryType: ReactionSkillType
) {
	val startTime = System.nanoTime()

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = -1, maxValue = DURATION)
	var clickedAfter = -1L

	@Suppress("unused")
	private constructor() : this(ReactionSkillType.MeleeDefense)

	fun wasPassed() = clickedAfter in MIN_CLICK_AFTER..MAX_CLICK_AFTER

	companion object {

		const val DURATION = 1040_000_000L
		const val RESULT_FADE_DURATION = 1000_000_000L
		const val FINAL_FADE_DURATION = 500_000_000L
		const val MIN_CLICK_AFTER = 580_000_000L
		const val MAX_CLICK_AFTER = 715_000_000L
	}
}
