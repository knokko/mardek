package mardek.assets.area

import mardek.assets.area.objects.AreaObjects

class ParsedArea(
	val width: Int,
	val height: Int,
	val tilesheetName: String,
	private val tileGrid: IntArray,
	val objects: AreaObjects,
	// TODO Loot
	val randomBattles: RandomAreaBattles?,
	val flags: AreaFlags,
	val properties: AreaProperties,
) {

	fun getTileId(x: Int, y: Int): Int? {
		if (x < 0 || y < 0 || x >= width || y >= height) return null
		return tileGrid[x + y * width]
	}
}
