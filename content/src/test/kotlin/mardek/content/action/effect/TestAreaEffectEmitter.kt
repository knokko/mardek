package mardek.content.action.effect

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class TestAreaEffectEmitter {

	@Test
	fun testWithNonZeroPeriod() {
		val emitter = AreaEffectsEmitter(
			firstDispatchAfter = 1000.milliseconds,
			period = 2000.milliseconds,
			maxDispatches = 4,
			maxLifetime = 5000.milliseconds,
			rings = emptyArray(),
		)

		assertEquals(IntRange.EMPTY, emitter.getRelevantDispatches(0.milliseconds))
		assertEquals(IntRange.EMPTY, emitter.getRelevantDispatches(999.milliseconds))
		assertEquals(0 .. 0, emitter.getRelevantDispatches(1000.milliseconds))
		assertEquals(0 .. 0, emitter.getRelevantDispatches(2999.milliseconds))
		assertEquals(0 .. 1, emitter.getRelevantDispatches(3000.milliseconds))
		assertEquals(0 .. 1, emitter.getRelevantDispatches(4999.milliseconds))
		assertEquals(0 .. 2, emitter.getRelevantDispatches(5000.milliseconds))
		assertEquals(0 .. 2, emitter.getRelevantDispatches(5999.milliseconds))
		assertEquals(1 .. 2, emitter.getRelevantDispatches(6000.milliseconds))
		assertEquals(1 .. 2, emitter.getRelevantDispatches(6999.milliseconds))
		assertEquals(1 .. 3, emitter.getRelevantDispatches(7000.milliseconds))
		assertEquals(1 .. 3, emitter.getRelevantDispatches(7999.milliseconds))
		assertEquals(2 .. 3, emitter.getRelevantDispatches(8000.milliseconds))
		assertEquals(2 .. 3, emitter.getRelevantDispatches(9999.milliseconds))
		assertEquals(3 .. 3, emitter.getRelevantDispatches(10000.milliseconds))
		assertEquals(3 .. 3, emitter.getRelevantDispatches(11999.milliseconds))
		assertEquals(IntRange.EMPTY, emitter.getRelevantDispatches(12000.milliseconds))
		assertEquals(IntRange.EMPTY, emitter.getRelevantDispatches(20000.milliseconds))
	}

	@Test
	fun testWithZeroPeriod() {
		val emitter = AreaEffectsEmitter(
			firstDispatchAfter = 1234.milliseconds,
			period = Duration.ZERO,
			maxDispatches = 1,
			maxLifetime = 500.milliseconds,
			rings = emptyArray(),
		)

		assertEquals(IntRange.EMPTY, emitter.getRelevantDispatches(1233.milliseconds))
		assertEquals(0 .. 0, emitter.getRelevantDispatches(1234.milliseconds))
		assertEquals(0 .. 0, emitter.getRelevantDispatches(1733.milliseconds))
		assertEquals(IntRange.EMPTY, emitter.getRelevantDispatches(1734.milliseconds))
		assertEquals(IntRange.EMPTY, emitter.getRelevantDispatches(9999.milliseconds))
	}
}
