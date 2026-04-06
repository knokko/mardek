package mardek.state.util

/**
 * Represents an axis-aligned rectangle in 2d space with integer coordinates.
 *
 * This class is mostly used by the renderer to divide the window in regions where it renders the different parts of
 * the game. It is also used to tell the state *where* it rendered certain UI elements (typically buttons).
 */
class Rectangle(
	/**
	 * The X-coordinate of the left edge/border.
	 * It is the first/lowest X-coordinate that is contained in this rectangle.
	 */
	val minX: Int,

	/**
	 * The Y-coordinate of the top edge/border.
	 * It is the first/lowest Y-coordinate that is contained in this rectangle.
	 */
	val minY: Int,

	/**
	 * The width of this rectangle (typically in pixels)
	 */
	val width: Int,

	/**
	 * The height of this rectangle (typically in pixels)
	 */
	val height: Int,
) {

	/**
	 * The X-coordinate *after* [maxX]. It is the first X-coordinate that is no longer contained in this rectangle.
	 */
	val boundX: Int
		get() = minX + width

	/**
	 * The Y-coordinate *after* [maxY]. It is the first Y-coordinate that is no longer contained in this rectangle.
	 */
	val boundY: Int
		get() = minY + height

	/**
	 * The X-coordinate of the right edge/border.
	 * It is the last/largest X-coordinate that is contained in this rectangle.
	 */
	val maxX: Int
		get() = boundX - 1

	/**
	 * The Y-coordinate of the bottom edge/border.
	 * It is the last/largest Y-coordinate that is contained in this rectangle.
	 */
	val maxY: Int
		get() = boundY - 1

	/**
	 * Checks whether the point `(x, y)` lays inside this rectangle
	 */
	fun contains(x: Int, y: Int) = x >= minX && y >= minY && x < boundX && y < boundY

	override fun toString() = "Rectangle(minX=$minX, minY=$minY, maxX=$maxX, maxY=$maxY)"
}
