package mardek.state.ingame.battle

import mardek.content.util.Time
import mardek.state.ingame.battle.combatant.ResourceBarHistory
import mardek.state.util.RenderTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TestResourceBarHistory {

	private fun timing(virtual: Duration) = RenderTiming(
		Time(virtual, 0L), 0L, Duration.ZERO
	)

	@Test
	fun testSmallDamage() {
		assertEquals(10, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.0
		).roundToInt())
		assertEquals(9, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.015
		).roundToInt())
		assertEquals(8, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.03
		).roundToInt())
		assertEquals(7, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.05
		).roundToInt())
		assertEquals(6, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.07
		).roundToInt())
		assertEquals(5, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.095
		).roundToInt())
		assertEquals(4, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.12
		).roundToInt())
		assertEquals(3, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.155
		).roundToInt())
		assertEquals(2, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 0.2
		).roundToInt())
		assertEquals(2, ResourceBarHistory.computeDisplayedValue(
			10.0, 2.0, 10.0
		).roundToInt())
		assertEquals(0.2, ResourceBarHistory.reachesNewValueAfter(10.0, 2.0), 0.1)
	}

	@Test
	fun testLargeHealing() {
		assertEquals(10, ResourceBarHistory.computeDisplayedValue(
			10.0, 9999.0, 0.0
		).roundToInt())
		assertEquals(1156, ResourceBarHistory.computeDisplayedValue(
			10.0, 9999.0, 0.1
		).roundToInt())
		assertEquals(7603, ResourceBarHistory.computeDisplayedValue(
			10.0, 9999.0, 1.0
		).roundToInt())
		assertEquals(9746, ResourceBarHistory.computeDisplayedValue(
			10.0, 9999.0, 2.0
		).roundToInt())
		assertEquals(9999, ResourceBarHistory.computeDisplayedValue(
			10.0, 9999.0, 5.0
		).roundToInt())
		assertEquals(9999, ResourceBarHistory.computeDisplayedValue(
			10.0, 9999.0, 60.0
		).roundToInt())
		assertEquals(2.9, ResourceBarHistory.reachesNewValueAfter(
			10.0, 9999.0
		), 0.1)
	}

	@Test
	fun testEmptyHistory() {
		val history = ResourceBarHistory()
		assertEquals(
			ResourceBarHistory.Result(123, null),
			history.get(123, timing(12.seconds))
		)
	}

	@Test
	fun testSingleEntryHistory() {
		val history = ResourceBarHistory()
		history.insert(10, 2, Time(5.seconds))
		assertEquals(
			ResourceBarHistory.Result(10, null),
			history.get(2, timing(1.seconds))
		)
		assertEquals(10, history.get(2, timing(5.seconds)).displayedValue)
		assertEquals(
			ResourceBarHistory.Result(
				2, ResourceBarHistory.RedBar(2, 10, 118)
			),
			history.get(2, timing(6.seconds))
		)
	}

	@Test
	fun testIndependentTwoEntryHistory() {
		val history = ResourceBarHistory()
		history.insert(2, 10, Time(1.seconds))
		history.insert(10, 12, Time(5.seconds))
		assertEquals(
			ResourceBarHistory.Result(2, null),
			history.get(12, timing(0.seconds))
		)
		assertEquals(2, history.get(12, timing(1.seconds)).displayedValue)
		assertEquals(
			ResourceBarHistory.Result(10, null),
			history.get(12, timing(3.seconds))
		)
		assertEquals(
			ResourceBarHistory.Result(10, null),
			history.get(12, timing(5.seconds))
		)
		assertEquals(
			ResourceBarHistory.Result(12, null),
			history.get(12, timing(6.seconds))
		)
	}

	@Test
	fun testConsistency() {
		assertEquals(
			ResourceBarHistory.reachesNewValueAfter(7.0, 10.0),
			ResourceBarHistory.reachesNewValueAfter(10.0, 7.0),
		)
		assertEquals(
			ResourceBarHistory.reachesNewValueAfter(3.0, 0.0),
			ResourceBarHistory.reachesNewValueAfter(10.0, 7.0),
		)
	}
}
