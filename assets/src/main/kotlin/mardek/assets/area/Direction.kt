package mardek.assets.area

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
enum class Direction(val deltaX: Int, val deltaY: Int, val abbreviation: String) {
	Down(0, 1, "s"),
	Up(0, -1, "n"),
	Right(1, 0, "e"),
	Left(-1, 0, "w");

	companion object {
		fun delta(deltaX: Int, deltaY: Int) = entries.find { it.deltaX == deltaX && it.deltaY == deltaY }
	}
}
