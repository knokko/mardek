package mardek.renderer

import org.joml.Matrix3x2f
import org.joml.Vector2f

fun main() {
	// Clipping math (X coordinate):
	// x1 * a + y1 * b + c = 0
	// x4 * a + y4 * b + c = 0
	// x2 * a + y2 * b + c = 1 -> x2 * a + y2 * b + c - 1 = 0
	// x3 * a + y3 * b + c = 1 -> x3 * a + y3 * b + c - 1 = 0

	// Clipping math:
	// outX = ax * inX + bx * inY + cx
	// outY = ay * inX + by * inY + cy
	//
	// (0, 0) gets transformed to (x4, y4) so cx = x4 and cy = y4
	// (0, 1) gets transformed to (x1, y1) which implies:
	//   ax * 0 + bx * 1 + cx = bx + x4 = x1 -> bx = x1 - x4
	//   ay * 0 + by * 1 + cy = by + y4 = y1 -> by = y1 - y4
	// (1, 1) gets transformed to (x2, y2) which implies:
	//   ax * 1 + bx * 1 + cx = ax + (x1 - x4) + x4 = ax + x1 = x2 -> ax = x2 - x1
	//   ay * 1 + by * 1 + cy = ay + (y1 - y4) + y4 = ay + y1 = y2 -> ay = y2 - y1

	val x1 = 5f
	val y1 = 5f

	val x2 = 10f
	val y2 = 10f

	val x3 = 5f
	val y3 = 15f

	val x4 = 0f
	val y4 = 10f

	val matrix1 = Matrix3x2f(
		x2 - x1, y2 - y1,
		x1 - x4, y1 - y4,
		x4, y4,
	)
	println("(0, 0) gets transformed to ${matrix1.transformPosition(0f, 0f, Vector2f())}")
	println("(0, 1) gets transformed to ${matrix1.transformPosition(0f, 1f, Vector2f())}")
	println("(1, 1) gets transformed to ${matrix1.transformPosition(1f, 1f, Vector2f())}")
	println("(1, 0) gets transformed to ${matrix1.transformPosition(1f, 0f, Vector2f())}")

	val inv = matrix1.invert(Matrix3x2f())
	println("(x1, y1) gets inverted to ${inv.transformPosition(x1, y1, Vector2f())}")
	println("(x2, y2) gets inverted to ${inv.transformPosition(x2, y2, Vector2f())}")
	println("(x3, y3) gets inverted to ${inv.transformPosition(x3, y3, Vector2f())}")
	println("(x4, y4) gets inverted to ${inv.transformPosition(x4, y4, Vector2f())}")
}