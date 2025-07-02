package mardek.content.area

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class TestDirection {

	@Test
	fun testExactDelta() {
		assertEquals(Direction.Left, Direction.exactDelta(-1, 0))
		assertNull(Direction.exactDelta(0, 0))
		assertNull(Direction.exactDelta(1, 1))
	}

	@Test
	fun testBestDelta() {
		assertNull(Direction.bestDelta(0, 0))
		assertEquals(Direction.Left, Direction.bestDelta(-1, 0))
		assertEquals(Direction.Left, Direction.bestDelta(-5, 0))
		assertEquals(Direction.Left, Direction.bestDelta(-5, 4))
		assertEquals(Direction.Down, Direction.bestDelta(-5, 6))
		assertEquals(Direction.Up, Direction.bestDelta(-5, -6))
		assertEquals(Direction.Up, Direction.bestDelta(5, -6))
		assertEquals(Direction.Right, Direction.bestDelta(7, -6))
	}
}
