package mardek.state.ingame.battle

import mardek.content.skill.ReactionSkillType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

class TestReactionChallenge {

	@Test
	fun testMissedChallenge() {
		val challenge = ReactionChallenge(ReactionSkillType.MeleeDefense)
		assertFalse(challenge.wasPassed())
		assertTrue(challenge.isPending())

		// Sleep until we are too late
		sleep(ReactionChallenge.MAX_CLICK_AFTER / 1000_000L + 1L)
		assertFalse(challenge.wasPassed())
		assertFalse(challenge.isPending())

		// Clicking late doesn't help
		challenge.click()
		assertFalse(challenge.wasPassed())
		assertFalse(challenge.isPending())
	}

	@Test
	fun testClickTooEarly() {
		val challenge = ReactionChallenge(ReactionSkillType.RangedAttack)
		challenge.click()
		assertFalse(challenge.wasPassed())
		assertFalse(challenge.isPending())

		// Clicking again doesn't help
		sleep(ReactionChallenge.MIN_CLICK_AFTER / 1000_000L)
		assertFalse(challenge.wasPassed())
		assertFalse(challenge.isPending())
	}

	@Test
	fun testPassChallenge() {
		var passedCounter = 0

		repeat(3) {
			val challenge = ReactionChallenge(ReactionSkillType.MeleeAttack)
			sleep(ReactionChallenge.MIN_CLICK_AFTER / 1000_000L)

			challenge.click()
			assertFalse(challenge.isPending())
			if (challenge.wasPassed()) passedCounter += 1
		}

		// Since sleeping is not entirely deterministic, we allow 3 attempts
		assertTrue(passedCounter > 0, "Expected $passedCounter > 0")
		if (passedCounter < 3) println("Passed $passedCounter / 3 trials")
	}
}
