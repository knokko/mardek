package mardek.state.ingame.battle

import mardek.content.stats.StatusEffect
import mardek.content.util.Time
import mardek.state.ingame.battle.combatant.StatusEffectHistory
import mardek.state.util.RenderTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class TestStatusEffectHistory {

	@Test
	fun testEmptyHistory() {
		val history = StatusEffectHistory()
		assertNull(history.get(RenderTiming(
			Time.ZERO, System.nanoTime(), Duration.ZERO
		)))
		assertNull(history.get(RenderTiming(
			Time.ZERO, System.nanoTime(), 1.seconds
		)))
		assertNull(history.get(RenderTiming(
			Time.zero(), System.nanoTime(), Duration.ZERO,
		)))
		assertNull(history.get(RenderTiming(
			Time.zero(), System.nanoTime(), 1.seconds,
		)))
		assertNull(history.get(RenderTiming(
			Time(10.milliseconds), System.nanoTime(), 1.seconds
		)))
	}

	@Test
	fun testAddSingleEffectWithoutExtrapolation() {
		val poison = StatusEffect()
		val history = StatusEffectHistory()
		history.add(poison)

		// Render the +poison indicator for the first time at (virtual=5 minutes, real=1 second)
		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 0f),
			history.get(RenderTiming(
				Time(5.minutes, 800_000_000L),
				1_000_000_000L, 50.milliseconds
			))
		)

		// Without extrapolation, the nano times should be ignored, and only the virtual campaign time should count
		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 0f),
			history.get(RenderTiming(Time(5.minutes, 1800_000_000L),
			5_000_000_000L, Duration.ZERO
			))
		)
		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 0.5f),
			history.get(RenderTiming(
				Time(5.minutes + 500.milliseconds, 1800_000_000L),
				5_000_000_000L, Duration.ZERO
			))
		)
		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 0.99f),
			history.get(RenderTiming(
				Time(5.minutes + 990.milliseconds, 1800_000_000L),
				5_000_000_000L, Duration.ZERO
			))
		)
		assertNull(history.get(RenderTiming(
			Time(5.minutes + 1.seconds, 1800_000_000L),
			5_000_000_000L, Duration.ZERO,
		)))
	}

	@Test
	fun testAddSingleEffectWithOnlyExtrapolation() {
		val poison = StatusEffect()
		val history = StatusEffectHistory()
		history.add(poison)

		// Render the +poison indicator for the first time at (virtual=5 minutes, real=1 second)
		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 0f),
			history.get(RenderTiming(
				Time(5.minutes, 800_000_000L),
				1_000_000_000L, 5.seconds,
			))
		)

		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 0.5f),
			history.get(RenderTiming(
				Time(5.minutes, 800_000_000L),
				1_500_000_000L, 5.seconds
			))
		)
		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 1f),
			history.get(RenderTiming(
			Time(5.minutes, 800_000_000L),
			1_999_999_999L, 5.seconds
			))
		)
		assertEquals(
			StatusEffectHistory.Current(poison, StatusEffectHistory.Type.Add, 1f),
			history.get(RenderTiming(
			Time(5.minutes, 800_000_000L),
			1_999_999_999L, 5.seconds
			))
		)

		assertNull(history.get(RenderTiming(
			Time(5.minutes, 800_000_000L),
			2_000_000_000L, 5.seconds
		)))
	}

	@Test
	fun testChain() {
		val sleep = StatusEffect()
		val poison = StatusEffect()
		val history = StatusEffectHistory()
		history.add(sleep)
		history.add(poison)

		fun createTiming(virtualTime: Duration) = RenderTiming(
			Time(virtualTime, 123456789L),
			123456789L, Duration.ZERO
		)

		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 0f
		), history.get(createTiming(100.nanoseconds)))
		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 1f
		), history.get(createTiming(1.seconds + 99.nanoseconds)))
		assertEquals(
			StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 0f
		), history.get(createTiming(1.seconds + 100.nanoseconds)))
		assertEquals(
			StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 1f
		), history.get(createTiming(2.seconds + 99.nanoseconds)))
		assertNull(history.get(createTiming(2.seconds + 100.nanoseconds)))

		history.remove(sleep)
		history.add(sleep)

		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 0f
		), history.get(createTiming(6.seconds)))
		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 1f
		), history.get(createTiming(7.seconds - 1.nanoseconds)))
		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 0f
		), history.get(createTiming(8.seconds)))
		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Add, 1f
		), history.get(createTiming(9.seconds - 1.nanoseconds)))
		assertNull(history.get(createTiming(9.seconds)))

		history.remove(sleep)
		history.remove(poison)
		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 0f
		), history.get(createTiming(10.seconds)))
		assertEquals(
			StatusEffectHistory.Current(
			sleep, StatusEffectHistory.Type.Remove, 1f
		), history.get(createTiming(11.seconds - 1.nanoseconds)))

		history.add(poison)
		assertEquals(
			StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Remove, 0f
		), history.get(createTiming(12.seconds)))
		assertEquals(
			StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Remove, 1f
		), history.get(createTiming(13.seconds - 1.nanoseconds)))

		assertEquals(
			StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 0f
		), history.get(createTiming(14.seconds)))
		assertEquals(
			StatusEffectHistory.Current(
			poison, StatusEffectHistory.Type.Add, 1f
		), history.get(createTiming(15.seconds - 1.nanoseconds)))
		assertNull(history.get(createTiming(15.seconds)))
	}
}
