package mardek.state.title

class AbsoluteRectangle(val minX: Int, val minY: Int, val width: Int, val height: Int) {

	val boundX: Int
		get() = minX + width

	val boundY: Int
		get() = minY + height

	val maxX: Int
		get() = boundX - 1

	val maxY: Int
		get() = boundY - 1

	fun contains(x: Int, y: Int) = x >= minX && y >= minY && x <= boundX && y <= boundY
}
