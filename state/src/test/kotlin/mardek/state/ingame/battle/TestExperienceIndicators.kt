package mardek.state.ingame.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class TestExperienceIndicators {

	@Test
	fun simpleTest() {
		val indicators = ExperienceIndicators()
		assertNull(indicators.getEntryToDisplay(100L))
		assertNull(indicators.getEntryToDisplay(200L))

		indicators.queuedAmount += 300
		assertEquals(
			ExperienceIndicators.Entry(1_000_000_000L, 300),
			indicators.getEntryToDisplay(1_000_000_000L),
		)

		indicators.queuedAmount += 200
		assertEquals(
			ExperienceIndicators.Entry(1_000_000_000L, 300),
			indicators.getEntryToDisplay(2_000_000_000L),
		)

		indicators.queuedAmount += 500

		assertEquals(
			ExperienceIndicators.Entry(1_000_000_000L, 300),
			indicators.getEntryToDisplay(2_400_000_000L),
		)

		assertEquals(
			ExperienceIndicators.Entry(2_600_000_000L, 700),
			indicators.getEntryToDisplay(2_600_000_000L),
		)

		assertEquals(
			ExperienceIndicators.Entry(2_600_000_000L, 700),
			indicators.getEntryToDisplay(4_050_000_000L),
		)

		assertNull(indicators.getEntryToDisplay(4_100_000_000L))
	}
}
