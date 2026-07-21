package mardek.state.util

import mardek.content.util.Time
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestRenderTiming {

	@Test
	fun testNow() {
		val timing = RenderTiming(
			Time(11.seconds, 5_000_000_000L),
			5_100_000_000L, 50.milliseconds,
		)
		assertEquals(Time(11.seconds, 5_100_000_000L), timing.now())
		assertEquals(Duration.ZERO, timing.elapsedTimeSince(timing.now()))
	}

	@Test
	fun testElapsedTime() {
		val timing = RenderTiming(
			Time(11.seconds, 5_000_000_000L),
			5_100_000_000L, 150.milliseconds,
		)

		// Time in the past: virtual difference is 6 seconds, nano time is irrelevant,
		// remaining 100ms come from timing.renderNanoTime - timing.initialNanoTime
		assertEquals(6100.milliseconds, timing.elapsedTimeSince(
			Time(5.seconds, 1234567L)
		))

		// Same virtual time as the previous case, but with different nano time: should yield the same result
		assertEquals(6100.milliseconds, timing.elapsedTimeSince(
			Time(5.seconds, 14_500_000_000L)
		))

		// Same virtual time as timing.stateTime, but a smaller nanoTime: stateTime.nanoTime leads
		assertEquals(100.milliseconds, timing.elapsedTimeSince(
			Time(11.seconds, 4_000_000_000L)
		))

		// Same virtual time as timing.stateTime, but a larger nanoTime: this nanoTime leads
		assertEquals(80.milliseconds, timing.elapsedTimeSince(
			Time(11.seconds, 5_020_000_000L)
		))

		// Same virtual time as timing.stateTime, but a much larger nanoTime: capped by extrapolationLimit
		assertEquals((-150).milliseconds, timing.elapsedTimeSince(
			Time(11.seconds, 6_000_000_000L)
		))

		// Larger virtual time than timing.stateTime: nanoTime should be ignored
		assertEquals((-2).seconds, timing.elapsedTimeSince(
			Time(13.seconds + 100.milliseconds, 123456789L)
		))
	}

	@Test
	fun testCappedElapsedTime() {
		val timing = RenderTiming(
			Time(11.seconds, 5_000_000_000L),
			5_100_000_000L, 50.milliseconds,
		)

		// Time in the past: virtual difference is 6 seconds, nano time is irrelevant.
		// The 100ms nanoTime difference is capped to 50ms
		// remaining 100ms come from timing.renderNanoTime - timing.initialNanoTime
		assertEquals(6050.milliseconds, timing.elapsedTimeSince(
			Time(5.seconds, 1234567L)
		))

		// Same virtual time as the previous case, but with different nano time: should yield the same result
		assertEquals(6050.milliseconds, timing.elapsedTimeSince(
			Time(5.seconds, 14_500_000_000L)
		))

		// Same virtual time as timing.stateTime, but a smaller nanoTime.
		// stateTime.nanoTime leads, but is capped to 50ms
		assertEquals(50.milliseconds, timing.elapsedTimeSince(
			Time(11.seconds, 4_000_000_000L)
		))

		// Same virtual time as timing.stateTime, but a larger nanoTime.
		// this nanoTime leads, but is capped to 50ms
		assertEquals(50.milliseconds, timing.elapsedTimeSince(
			Time(11.seconds, 5_020_000_000L)
		))

		// Larger virtual time than timing.stateTime: nanoTime should be ignored.
		// The 100ms between initialNanoTime and renderNanoTime is capped to 50ms
		assertEquals((-2).seconds - 50.milliseconds, timing.elapsedTimeSince(
			Time(13.seconds + 100.milliseconds, 123456789L)
		))
	}

	@Test
	fun testInterpolate() {
		val timing = RenderTiming(
			Time(6.seconds, 8_000_000_000L),
			8_000_000_000L, 50.milliseconds
		)

		assertEquals(3, timing.interpolate(
			Time(7.seconds), 3, 1.seconds, 9, true
		))
		assertEquals(-3, timing.interpolate(
			Time(7.seconds), 3, 1.seconds, 9, false
		))
		assertEquals(3, timing.interpolate(
			Time(6.seconds), 3, 1.seconds, 9, true
		))
		assertEquals(6, timing.interpolate(
			Time(6.seconds - 500.milliseconds), 3,
			1.seconds, 9, true,
		))
		assertEquals(9, timing.interpolate(
			Time(5.seconds), 3,
			1.seconds, 9, true,
		))
		assertEquals(9, timing.interpolate(
			Time(4.seconds), 3,
			1.seconds, 9, true,
		))
		assertEquals(15, timing.interpolate(
			Time(4.seconds), 3,
			1.seconds, 9, false,
		))
	}

	@Test
	fun testOscillate() {
		val timing = RenderTiming(
			Time(6.seconds, 8_000_000_000L),
			8_000_000_000L, 50.milliseconds
		)

		val margin = 0.001f
		assertEquals(5f, timing.oscillate(5f, 8f, 2.seconds), margin)

		assertEquals(5.6f, timing.oscillate(
			5f, 8f, 2.seconds, Time(200.milliseconds)
		), margin)
		assertEquals(6.5f, timing.oscillate(
			5f, 8f, 2.seconds, Time(500.milliseconds)
		), margin)
		assertEquals(8f, timing.oscillate(
			5f, 8f, 2.seconds, Time(1.seconds)
		), margin)
		assertEquals(7f, timing.oscillate(
			5f, 8f, 2.seconds, Time(4.seconds / 3)
		), margin)

		assertEquals(5f, timing.oscillate(
			5f, 8f, 2.seconds, Time(10.seconds)
		), margin)
		assertEquals(8f, timing.oscillate(
			5f, 8f, 2.seconds, Time(9.seconds)
		), margin)
		assertEquals(6.5f, timing.oscillate(
			5f, 8f, 2.seconds, Time(6500.milliseconds)
		), margin)
		assertEquals(6f, timing.oscillate(
			5f, 8f, 2.seconds, Time(6.seconds + 1.seconds / 3)
		), margin)
	}

	@Test
	fun testAlternate() {
		val timing = RenderTiming(
			Time(6.seconds, 8_000_000_000L),
			8_000_000_000L, 0.milliseconds
		)

		assertEquals(5, timing.alternateIntegers(
			3, 5.seconds, 5, Time(6.seconds)
		))
		assertEquals(5, timing.alternateIntegers(
			3, 5.seconds, 5, Time(1001.milliseconds)
		))
		assertEquals(6, timing.alternateIntegers(
			3, 5.seconds, 5, Time(1.seconds)
		))
		assertEquals(6, timing.alternateIntegers(
			3, 5.seconds, 5, Time((-3999).milliseconds)
		))
		assertEquals(7, timing.alternateIntegers(
			3, 5.seconds, 5, Time((-4).seconds)
		))
		assertEquals(7, timing.alternateIntegers(
			3, 5.seconds, 5, Time((-8999).milliseconds)
		))

		assertEquals(5, timing.alternateIntegers(
			3, 5.seconds, 5, Time(21.seconds)
		))
		assertEquals(5, timing.alternateIntegers(
			3, 5.seconds, 5, Time(16001.milliseconds)
		))
		assertEquals(6, timing.alternateIntegers(
			3, 5.seconds, 5, Time(16.seconds)
		))
		assertEquals(6, timing.alternateIntegers(
			3, 5.seconds, 5, Time(11001.milliseconds)
		))
		assertEquals(7, timing.alternateIntegers(
			3, 5.seconds, 5, Time(11.seconds)
		))
		assertEquals(7, timing.alternateIntegers(
			3, 5.seconds, 5, Time(6001.milliseconds)
		))

		assertEquals(5, timing.alternateIntegers(
			3, 5.seconds, 5, Time(36.seconds)
		))
		assertEquals(7, timing.alternateIntegers(
			3, 5.seconds, 5, Time((-23999).milliseconds)
		))
	}
}
