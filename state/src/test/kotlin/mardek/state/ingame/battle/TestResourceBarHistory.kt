package mardek.state.ingame.battle

import mardek.content.stats.Element
import mardek.content.util.Time
import mardek.state.ingame.battle.combatant.ResourceBarHistory
import mardek.state.util.RenderTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class TestResourceBarHistory {

	private fun timing(virtual: Duration) = RenderTiming(
		Time(virtual, 0L), 0L, Duration.ZERO
	)

//	@Test
//	fun testSmallDamage() {
//		val entry = ResourceBarHistory.Entry(
//			10, 2, Time(10.seconds, 123L), Element()
//		)
//		assertEquals(10, entry.computeDisplayedValueAt(timing(10.seconds)))
//		for (counter in 1 until 10) {
//			println(entry.computeDisplayedValueAt(timing(10.seconds + 100.milliseconds * counter)))
//		}
//		assertEquals(2, entry.computeDisplayedValueAt(timing(11.seconds)))
//		assertEquals(2, entry.computeDisplayedValueAt(timing(20.seconds)))
//		assertEquals(0.55, entry.reachesNewValueAfter().toDouble(DurationUnit.SECONDS), 0.1)
//	}
//
//	@Test
//	fun testLargeHealing() {
//		val entry = ResourceBarHistory.Entry(
//			10, 9999, Time(5.seconds, 123L), Element()
//		)
//		assertEquals(10, entry.computeDisplayedValueAt(timing(5.seconds)))
//		for (counter in 1 until 100) {
//			println(entry.computeDisplayedValueAt(timing(5.seconds + 100.milliseconds * counter)))
//		}
//		assertEquals(9999, entry.computeDisplayedValueAt(timing(10.seconds)))
//		assertEquals(9999, entry.computeDisplayedValueAt(timing(60.seconds)))
//		assertEquals(4.8, entry.reachesNewValueAfter().toDouble(DurationUnit.SECONDS), 0.1)
//	}
//
//	@Test
//	fun testEmptyHistory() {
//		val history = ResourceBarHistory()
//		assertEquals(123, history.computeDisplayedValue(123, timing(12.seconds)))
//	}
//
//	@Test
//	fun testSingleEntryHistory() {
//		val history = ResourceBarHistory()
//		history.insert(10, 2, Time(5.seconds), Element())
//		assertEquals(10, history.computeDisplayedValue(2, timing(1.seconds)))
//		assertEquals(10, history.computeDisplayedValue(2, timing(5.seconds)))
//		assertEquals(2, history.computeDisplayedValue(2, timing(6.seconds)))
//	}
//
//	@Test
//	fun testIndependentTwoEntryHistory() {
//		val history = ResourceBarHistory()
//		history.insert(2, 10, Time(1.seconds), Element())
//		history.insert(10, 12, Time(5.seconds), Element())
//		assertEquals(2, history.computeDisplayedValue(12, timing(0.seconds)))
//		assertEquals(2, history.computeDisplayedValue(12, timing(1.seconds)))
//		assertEquals(10, history.computeDisplayedValue(12, timing(3.seconds)))
//		assertEquals(10, history.computeDisplayedValue(12, timing(5.seconds)))
//		assertEquals(12, history.computeDisplayedValue(12, timing(6.seconds)))
//		assertEquals(12, history.computeDisplayedValue(12, timing(60.seconds)))
//	}

	// TODO CHAP1 Uncomment the other tests
	@Test
	fun testSunderDamageDuration() {
		assertEquals(
			ResourceBarHistory.reachesNewValueAfter(7.0, 10.0),
			ResourceBarHistory.reachesNewValueAfter(10.0, 7.0),
		)
		assertEquals(
			ResourceBarHistory.reachesNewValueAfter(3.0, 0.0),
			ResourceBarHistory.reachesNewValueAfter(10.0, 7.0),
		)
		println(ResourceBarHistory.reachesNewValueAfter(7.0, 10.0)) // Should be ~0.1 seconds
		println(ResourceBarHistory.reachesNewValueAfter(76.0, 67.0)) // Should be ~0.3 seconds
		println(ResourceBarHistory.reachesNewValueAfter(70.0, 47.0)) // Should be ~0.35 seconds
		println(ResourceBarHistory.reachesNewValueAfter(44000.0, 43417.0)) // Should be ~1.2 seconds
		println(ResourceBarHistory.reachesNewValueAfter(14730.0, 7554.0)) // Should be ~3.6 seconds
	}
}
