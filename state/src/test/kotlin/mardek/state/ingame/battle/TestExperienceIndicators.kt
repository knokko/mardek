package mardek.state.ingame.battle

import mardek.content.util.Time
import mardek.state.util.RenderTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestExperienceIndicators {

	@Test
	fun simpleTest() {
		val indicators = ExperienceIndicators()
		assertNull(indicators.getEntryToDisplay(RenderTiming(
			Time.ZERO, 12345L, 123.seconds
		)))
		assertNull(indicators.getEntryToDisplay(RenderTiming(
			Time(123.seconds, 123456L),
			123456L, 10.seconds
		)))

		indicators.queuedAmount += 300
		assertEquals(
			ExperienceIndicators.Entry(Time(10.seconds, 500L), 300),
			indicators.getEntryToDisplay(RenderTiming(
				Time(10.seconds, 400L),
				500L, 1.milliseconds,
			)),
		)

		indicators.queuedAmount += 200
		assertEquals(
			ExperienceIndicators.Entry(Time(10.seconds, 500L), 300),
			indicators.getEntryToDisplay(RenderTiming(
				Time(11.seconds, 800L),
				800L, 1.milliseconds,
			)),
		)

		indicators.queuedAmount += 500

		assertEquals(
			ExperienceIndicators.Entry(Time(10.seconds, 500L), 300),
			indicators.getEntryToDisplay(RenderTiming(
				Time(11.seconds, 800L),
				400_000_800L, 800.milliseconds,
			)),
		)

		assertEquals(
			ExperienceIndicators.Entry(Time(11.seconds, 600_000_800L), 700),
			indicators.getEntryToDisplay(RenderTiming(
				Time(11.seconds, 800L),
				600_000_800L, 800.milliseconds,
			)),
		)

		assertEquals(
			ExperienceIndicators.Entry(Time(11.seconds, 600_000_800L), 700),
			indicators.getEntryToDisplay(RenderTiming(
				Time(12.seconds, 1_000_000_000L),
				1_499_999_999L, 800.milliseconds,
			)),
		)

		assertNull(indicators.getEntryToDisplay(RenderTiming(
			Time(12.seconds, 1_000_000_000L),
			1_500_000_001L, 800.milliseconds,
		)))
	}
}
