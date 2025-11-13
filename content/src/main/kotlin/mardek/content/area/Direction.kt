package mardek.content.area

import com.github.knokko.bitser.BitEnum
import kotlin.math.abs

/**
 * The 4 basic directions: down, up, right, and left
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class Direction(val deltaX: Int, val deltaY: Int, val abbreviation: String) {
	/**
	 * Down/south (positive Y)
	 */
	Down(0, 1, "s"),

	/**
	 * Up/north (negative Y)
	 */
	Up(0, -1, "n"),

	/**
	 * Right/east (positive X)
	 */
	Right(1, 0, "e"),

	/**
	 * Left/west (negative X)
	 */
	Left(-1, 0, "w");

	companion object {

		/**
		 * Find the `Direction` with the given `deltaX` and `deltaY`. Returns `null` when no such direction exists.
		 * For instance, `exactDelta(1, 0)` would return `Direction.Right`, whereas `exactDelta(2, 0)` returns `null`.
		 */
		fun exactDelta(deltaX: Int, deltaY: Int) = entries.find { it.deltaX == deltaX && it.deltaY == deltaY }

		/**
		 * Find the `Direction` that is closest to the vector `(deltaX, deltaY)`, with arbitrary tie-breaking.
		 * This method will return `null` if and only if `deltaX == 0 && deltaY == 0`. Examples:
		 * - `bestDelta(1, 0)` returns `Direction.Right`
		 * - `bestDelta(-10, 9)` returns `Direction.Left`
		 * - `bestDelta(-10, 11)` returns `Direction.Down`
		 */
		fun bestDelta(deltaX: Int, deltaY: Int): Direction? {
			if (deltaX == 0 && deltaY == 0) return null
			return if (abs(deltaX) > abs(deltaY)) {
				if (deltaX > 0) Right else Left
			} else {
				if (deltaY > 0) Down else Up
			}
		}
	}
}
