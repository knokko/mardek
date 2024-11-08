package mardek.assets.area

enum class Direction(val deltaX: Int, val deltaY: Int, val abbreviation: String) {
	Down(0, 1, "s"),
	Up(0, -1, "n"),
	Right(1, 0, "e"),
	Left(-1, 0, "w"),
}
