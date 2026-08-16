package mardek.state.ingame.battle

import mardek.content.particle.ParticleEffect
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import mardek.content.util.Time
import mardek.state.ingame.battle.combatant.DamageIndicatorHistory
import mardek.state.util.RenderTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestIndicatorHistory {

	@Test
	fun testEmptyHistory() {
		val history = DamageIndicatorHistory()
		val timing = RenderTiming(
			Time(123.seconds), 1_000_000_000L, 50.milliseconds
		)
		assertNull(history.mostRecentDamageTakenAt(timing))
		assertEquals(0, history.get(timing).size)
	}

	private fun simpleTiming(sinceOrigin: Duration) = RenderTiming(
		Time(sinceOrigin), 0L, Duration.ZERO
	)

	@Test
	fun testWithSingleEntry() {
		val history = DamageIndicatorHistory()
		val element = Element()
		history.addOnTurnIndicator(
			StatusEffect.TurnDamage(0.1f, element, ParticleEffect(), 123),
			20, Time(20.seconds, 100_000_000L),
		)

		val before = simpleTiming(19900.milliseconds)
		val rightAt = simpleTiming(20.seconds)
		val secondAfter = simpleTiming(21.seconds)
		val tooLate = simpleTiming(22.seconds)

		assertEquals(0, history.get(before).size)
		assertNull(history.mostRecentDamageTakenAt(before))

		assertEquals(20.seconds, history.mostRecentDamageTakenAt(rightAt)!!.virtual)
		val entriesRightAt = history.get(rightAt)

		assertEquals(20.seconds, history.mostRecentDamageTakenAt(secondAfter)!!.virtual)
		val entriesSecondAfter = history.get(secondAfter)

		assertNull(history.mostRecentDamageTakenAt(tooLate))
		assertEquals(0, history.get(tooLate).size)

		assertEquals(1, entriesRightAt.size)
		assertEquals(1, entriesSecondAfter.size)

		val entryAt = entriesRightAt[0]
		val entryAfter = entriesSecondAfter[0]

		for (entry in arrayOf(entryAt, entryAfter)) {
			assertSame(element, entry.element)
			assertEquals(20, entry.amount)
			assertEquals(DamageIndicatorHistory.ResultType.LoseHealth, entry.type)
			assertEquals(20.seconds, entry.insertionTime.virtual)
			assertEquals(123, entry.blinkColor)
			assertEquals(0f, entry.blinkIntensity, 0.01f)
			assertEquals(0f, entry.relativeY, 0.01f)
			assertEquals(1f, entry.heightFactor, 0.01f)
		}

		assertEquals(0f, entryAt.opacity, 0.01f)
		assertEquals(1f, entryAfter.opacity, 0.01f)
	}

	@Test
	fun testWithTwoEntries() {
		val element1 = Element()
		val element2 = Element()

		val history = DamageIndicatorHistory()
		history.addOnTurnIndicator(
			StatusEffect.TurnDamage(0.1f, element1, ParticleEffect(), 123),
			20, Time(20.seconds, 100_000_000L),
		)
		history.addOnTurnIndicator(
			StatusEffect.TurnDamage(-0.1f, element2, ParticleEffect(), 1234),
			-25, Time(25.seconds, 300_000_000L),
		)

		val entries = history.get(RenderTiming(
			Time(25100.milliseconds, 300_000_000L),
			350_000_000L, Duration.ZERO,
		))
		assertEquals(1, entries.size)
		val entry = entries[0]
		assertSame(element2, entry.element)
		assertEquals(25, entry.amount)
		assertEquals(DamageIndicatorHistory.ResultType.GainHealth, entry.type)
		assertEquals(25.seconds, entry.insertionTime.virtual)
		assertEquals(1234, entry.blinkColor)
		assertEquals(0.43f, entry.blinkIntensity, 0.01f)
		assertEquals(0.8f, entry.relativeY, 0.01f)
		assertEquals(1f, entry.heightFactor)
		assertEquals(1f, entry.opacity, 0.01f)
	}
}
