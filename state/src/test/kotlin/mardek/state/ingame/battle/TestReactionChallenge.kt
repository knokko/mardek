package mardek.state.ingame.battle

import mardek.content.skill.ReactionSkillType
import mardek.content.util.Time
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TestReactionChallenge {

	@Test
	fun testMissedChallenge() {
		val challenge = ReactionChallenge(ReactionSkillType.MeleeDefense, Time(5.minutes))
		assertFalse(challenge.wasPassed())

		assertFalse(challenge.isPending(Time(5.minutes - 1.seconds)))
		assertTrue(challenge.isPending(Time(5.minutes)))
		assertTrue(challenge.isPending(Time(5.minutes + ReactionChallenge.MAX_CLICK_AFTER)))

		assertFalse(challenge.wasPassed())
		assertFalse(challenge.isPending(Time(5.minutes + ReactionChallenge.MAX_CLICK_AFTER + 1.milliseconds)))

		// Clicking late doesn't help
		challenge.click(Time(5.minutes + ReactionChallenge.MAX_CLICK_AFTER + 1.milliseconds))
		assertFalse(challenge.wasPassed())
	}

	@Test
	fun testClickTooEarly() {
		val challenge = ReactionChallenge(ReactionSkillType.RangedAttack, Time(2.minutes))
		challenge.click(Time(2.minutes + 10.milliseconds))
		assertFalse(challenge.wasPassed())
		assertFalse(challenge.isPending(Time(2.minutes + 10.milliseconds)))
		assertFalse(challenge.isPending(Time(2.minutes + ReactionChallenge.MIN_CLICK_AFTER)))

		// Clicking again doesn't help
		challenge.click(Time(2.minutes) + ReactionChallenge.MIN_CLICK_AFTER)
		assertFalse(challenge.wasPassed())
		assertFalse(challenge.isPending(Time(2.minutes) + ReactionChallenge.MIN_CLICK_AFTER))
	}

	@Test
	fun testPassChallenge() {
		val challenge = ReactionChallenge(ReactionSkillType.MeleeAttack, Time(1.hours))
		challenge.click(Time(1.hours + ReactionChallenge.MIN_CLICK_AFTER))
		assertFalse(challenge.isPending(Time(1.hours + ReactionChallenge.MIN_CLICK_AFTER)))
		assertTrue(challenge.wasPassed())
	}
}
