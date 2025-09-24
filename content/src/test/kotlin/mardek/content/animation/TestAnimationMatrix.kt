package mardek.content.animation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TestAnimationMatrix {

	@Test
	fun testEquals() {
		val withoutScale1 = AnimationMatrix(
			1f, 2f, 3f, 4f,
			false, 5f, 6f,
		)
		val withoutScale2 = AnimationMatrix(
			1f, 2f, 3f, 4f,
			false, -1f, 2f,
		)
		val withoutScale3 = AnimationMatrix(
			1f, 2f, 3f, 4f,
			true, 1f, 1f,
		)
		val withoutScale4 = AnimationMatrix(
			5f, 5f, 5f, 5f,
			true, 1f, 1f,
		)

		assertEquals(withoutScale1, withoutScale2)
		assertEquals(withoutScale1, withoutScale3)
		assertNotEquals(withoutScale1, withoutScale4)

		assertEquals(withoutScale2, withoutScale1)
		assertEquals(withoutScale2, withoutScale3)
		assertNotEquals(withoutScale2, withoutScale4)

		assertEquals(withoutScale3, withoutScale1)
		assertEquals(withoutScale3, withoutScale2)
		assertNotEquals(withoutScale3, withoutScale4)

		assertNotEquals(withoutScale4, withoutScale1)
		assertNotEquals(withoutScale4, withoutScale2)
		assertNotEquals(withoutScale4, withoutScale3)

		val withScale1 = AnimationMatrix(
			1f, 2f, 3f, 4f,
			true, 5f, 6f,
		)
		val withScale2 = AnimationMatrix(
			8f, 8f, 8f, 8f,
			true, 5f, 6f,
		)
		assertEquals(withScale1, withScale1)
		assertNotEquals(withScale1, withScale2)

		for (matrix in arrayOf(withoutScale1, withoutScale2, withoutScale3, withoutScale4)) {
			assertNotEquals(withScale1, matrix)
			assertNotEquals(withScale2, matrix)
			assertNotEquals(matrix, withScale1)
			assertNotEquals(matrix, withScale2)
		}
	}
}
