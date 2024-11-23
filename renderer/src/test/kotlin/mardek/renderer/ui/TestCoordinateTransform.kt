package mardek.renderer.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestCoordinateTransform {

	@Test
	fun testSquare4() {
		testSquare4(SpaceLayout.Simple)
		testSquare4(SpaceLayout.GrowRight)
		// TODO Test GrowUp and GrowDown
	}

	private fun testSquare4(layout: SpaceLayout) {
		val transform = CoordinateTransform.create(layout, 4, 4)

		assertEquals(0, transform.transformX(0.01f))
		assertEquals(0, transform.transformX(0.24f))
		assertEquals(1, transform.transformX(0.26f))
		assertEquals(1, transform.transformX(0.49f))
		assertEquals(2, transform.transformX(0.51f))
		assertEquals(2, transform.transformX(0.74f))
		assertEquals(3, transform.transformX(0.76f))
		assertEquals(3, transform.transformX(0.99f))
		assertEquals(4, transform.transformX(1.01f))

		assertEquals(3, transform.transformY(0.01f))
		assertEquals(3, transform.transformY(0.24f))
		assertEquals(2, transform.transformY(0.26f))
		assertEquals(2, transform.transformY(0.49f))
		assertEquals(1, transform.transformY(0.51f))
		assertEquals(1, transform.transformY(0.74f))
		assertEquals(0, transform.transformY(0.76f))
		assertEquals(0, transform.transformY(0.99f))
		assertEquals(-1, transform.transformY(1.01f))

		for (transformSize in arrayOf(transform::transformWidth, transform::transformHeight)) {
			assertEquals(0, transformSize(-0.12f))
			assertEquals(0, transformSize(0f))
			assertEquals(0, transformSize(0.12f))
			assertEquals(1, transformSize(0.13f))
			assertEquals(1, transformSize(0.37f))
			assertEquals(2, transformSize(0.38f))
			assertEquals(2, transformSize(0.62f))
			assertEquals(3, transformSize(0.63f))
			assertEquals(3, transformSize(0.87f))
			assertEquals(4, transformSize(0.88f))
			assertEquals(4, transformSize(1.12f))
		}
	}

	@Test
	fun testLong4Simple() {
		val transform = CoordinateTransform.create(SpaceLayout.Simple, 8, 4)

		assertEquals(0, transform.transformX(0.01f))
		assertEquals(3, transform.transformX(0.49f))
		assertEquals(4, transform.transformX(0.51f))
		assertEquals(7, transform.transformX(0.99f))

		assertEquals(3, transform.transformY(0.01f))
		assertEquals(2, transform.transformY(0.49f))
		assertEquals(1, transform.transformY(0.51f))
		assertEquals(0, transform.transformY(0.99f))

		assertEquals(4, transform.transformWidth(0.56f))
		assertEquals(5, transform.transformWidth(0.57f))

		assertEquals(2, transform.transformHeight(0.62f))
		assertEquals(3, transform.transformHeight(0.63f))
	}

	@Test
	fun testLong4GrowRight() {
		val transform = CoordinateTransform.create(SpaceLayout.GrowRight, 8, 4)

		assertEquals(0, transform.transformX(0.01f))
		assertEquals(1, transform.transformX(0.49f))
		assertEquals(2, transform.transformX(0.51f))
		assertEquals(3, transform.transformX(0.99f))

		assertEquals(3, transform.transformY(0.01f))
		assertEquals(2, transform.transformY(0.49f))
		assertEquals(1, transform.transformY(0.51f))
		assertEquals(0, transform.transformY(0.99f))

		assertEquals(2, transform.transformWidth(0.62f))
		assertEquals(3, transform.transformWidth(0.63f))
		assertEquals(2, transform.transformHeight(0.62f))
		assertEquals(3, transform.transformHeight(0.63f))
	}

	@Test
	fun testTransformFullRectangle() {
		val transform = CoordinateTransform.create(SpaceLayout.Simple, 100, 300)
		val result = transform.transform(0f, 0f, 1f, 1f)
		assertEquals(0, result.minX)
		assertEquals(0, result.minY)
		assertEquals(100, result.width)
		assertEquals(300, result.height)
		assertEquals(99, result.maxX)
		assertEquals(299, result.maxY)
		assertEquals(100, result.boundX)
		assertEquals(300, result.boundY)
	}

	@Test
	fun testTransformSmallRectangle() {
		val transform = CoordinateTransform.create(SpaceLayout.Simple, 100, 100)

		val result = transform.transform(0.101f, 0.201f, 0.3f, 0.1f)
		assertEquals(10, result.minX)
		assertEquals(69, result.minY)
		assertEquals(30, result.width)
		assertEquals(10, result.height)
		assertEquals(39, result.maxX)
		assertEquals(78, result.maxY)
		assertEquals(40, result.boundX)
		assertEquals(79, result.boundY)
	}
}
