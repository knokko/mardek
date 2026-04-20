package mardek.content.action.effect

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TestAreaEffectFloat {

	@Test
	fun simpleTests() {
		val function = AreaEffectFloat(initial = -1.5f, linear = 2f, min = -1f, max = 10f)

		// Clamped by min = -1
		assertEquals(-1f, function.getAny(Duration.ZERO))

		// Clamped to 0 by the non-negative requirement
		assertEquals(0f, function.getNonNegative(Duration.ZERO))

		// Clamped to 0 due to the restricted color component range of [0, 1]
		assertEquals(0f, function.getColorComponent(Duration.ZERO))

		// Not clamped at all
		assertEquals(0.5f, function.getAny(1.seconds))
		assertEquals(0.5f, function.getNonNegative(1.seconds))
		assertEquals(0.5f, function.getColorComponent(1.seconds))

		assertEquals(2.5f, function.getAny(2.seconds))
		assertEquals(2.5f, function.getNonNegative(2.seconds))

		// Clamped to 1 due to the restricted color component range of [0, 1]
		assertEquals(1f, function.getColorComponent(2.seconds))

		// Clamped by max = 10
		assertEquals(10f, function.getAny(8.seconds))
		assertEquals(10f, function.getNonNegative(8.seconds))

		// Clamped to 1 due to the restricted color component range of [0, 1]
		assertEquals(1f, function.getColorComponent(8.seconds))
	}
}
