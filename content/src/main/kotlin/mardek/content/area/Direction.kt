package mardek.content.area

import com.github.knokko.bitser.BitEnum
import kotlin.math.abs

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class Direction(val deltaX: Int, val deltaY: Int, val abbreviation: String) {
	Down(0, 1, "s"),
	Up(0, -1, "n"),
	Right(1, 0, "e"),
	Left(-1, 0, "w");

	companion object {
		fun exactDelta(deltaX: Int, deltaY: Int) = entries.find { it.deltaX == deltaX && it.deltaY == deltaY }

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
