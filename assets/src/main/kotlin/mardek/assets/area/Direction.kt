package mardek.assets.area

enum class Direction(val deltaX: Int, val deltaY: Int, val abbreviation: String) {
	Right(1, 0, "e"),
	Up(0, -1, "n"),
	Left(-1, 0, "w"),
	Down(0, 1, "s")
}
