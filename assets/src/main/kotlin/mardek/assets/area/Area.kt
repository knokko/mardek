package mardek.assets.area

class Area(
	val name: String, val tilesheetName: String, val width: Int, val height: Int,
	private val tileGrid: IntArray, val tileList: List<Tile>
) {

	private fun index(x: Int, y: Int): Int? {
		if (x < 0 || y < 0 || x >= width || y >= height) return null
		return x + y * width
	}

	fun canWalkAt(x: Int, y: Int): Boolean {
		val colliderIndex = index(x, y) ?: return false
		return tileList[tileGrid[colliderIndex]].canMoveTo
	}

	fun getTileAt(x: Int, y: Int) = tileList[tileGrid[index(x, y)!!]]
}
