package mardek.state.ingame.battle

import mardek.content.sprite.BcSprite
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestCustomParticle {

	@Test
	fun testPoison() {
		val poison = CustomParticle.poison(MonsterCombatantState(), BcSprite())
		assertTrue(poison.startSize >= 4f)

		// Simulate 60 flash frames = 2 seconds:
		var flashY = poison.startY
		var flashGrowth = 1.3f
		var flashWidth = poison.startSize
		for (frame in 0 until 60) {
			val passedNanoTime = frame * 1000_000_000L / 30
			val time = poison.spawnTime + passedNanoTime
			assertEquals(flashY, poison.getY(time), 0.01f)
			assertEquals(flashWidth, poison.getSize(time), 0.01f)
			assertEquals(poison.startX, poison.getX(time))
			assertEquals(1f, poison.getOpacity(time))

			flashWidth *= flashGrowth
			flashGrowth -= 0.05f
			flashY -= 1f
		}
	}

	@Test
	fun testBlind() {
		val blind = CustomParticle.blind(MonsterCombatantState(), BcSprite())

		val frameLength = 33_333_333L
		for (frame in 0 .. 60) {
			val time = System.nanoTime() + frame * frameLength
			assertEquals(blind.startX, blind.getX(time))
			assertEquals(blind.startY, blind.getY(time))
			assertEquals(blind.startSize, blind.getSize(time))
		}

		assertEquals(0.8f, blind.getOpacity(blind.spawnTime), 0.01f)
		assertEquals(0.72f, blind.getOpacity(blind.spawnTime + frameLength), 0.01f)
		assertEquals(0.64f, blind.getOpacity(blind.spawnTime + 2 * frameLength), 0.01f)
		assertEquals(0.08f, blind.getOpacity(blind.spawnTime + 9 * frameLength), 0.01f)
	}

	@Test
	fun testSleep() {
		val sleep = CustomParticle.sleep(MonsterCombatantState(), BcSprite())
		assertTrue(sleep.rotation >= 0f)
		assertTrue(sleep.rotation < 31f)
		assertTrue(sleep.startSize >= 4f)

		val frameLength = 33_333_333L
		val frame1 = sleep.spawnTime + frameLength
		assertEquals(sleep.startX + 0.5f, sleep.getX(frame1), 0.01f)
		assertEquals(sleep.startY - 0.6f, sleep.getY(frame1), 0.01f)
		assertEquals(sleep.startSize * 1.04f, sleep.getSize(frame1), 0.01f)
		assertEquals(0.98f, sleep.getOpacity(frame1), 0.01f)

		val frame10 = sleep.spawnTime + 10 * frameLength
		assertEquals(sleep.startX + 5f, sleep.getX(frame10), 0.01f)
		assertEquals(sleep.startY - 6f, sleep.getY(frame10), 0.01f)
		assertEquals(0.8f, sleep.getOpacity(frame10), 0.01f)

		var size = sleep.startSize
		for (frame in 0 until 50) {
			val time = sleep.spawnTime + frame * frameLength
			assertEquals(size, sleep.getSize(time), 0.01f)
			size *= 1.04f
		}
	}

	@Test
	fun testParalysis() {
		val paralysis = CustomParticle.paralysis(MonsterCombatantState(), BcSprite())
		assertTrue(paralysis.startSize >= 4f)

		val frameLength = 33_333_333L
		for (frame in 0 until 50) {
			val time = paralysis.spawnTime + frame * frameLength
			assertEquals(paralysis.startX, paralysis.getX(time))
			assertEquals(paralysis.startY, paralysis.getY(time))
			assertEquals(paralysis.startSize, paralysis.getSize(time))
			if (frame <= 1) assertEquals(1f, paralysis.getOpacity(time))
			else assertTrue(paralysis.getOpacity(time) <= 0f)
		}
	}
}
