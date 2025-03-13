package mardek.importer.area

import mardek.content.area.*
import mardek.content.area.objects.AreaObjects

class ParsedArea(
	val width: Int,
	val height: Int,
	val tilesheet: ParsedTilesheet,
	val tileGrid: IntArray,
	val objects: AreaObjects,
	val chests: List<Chest>,
	val randomBattles: RandomAreaBattles?,
	val flags: AreaFlags,
	val properties: AreaProperties,
) {

	fun getTileId(x: Int, y: Int): Int? {
		if (x < 0 || y < 0 || x >= width || y >= height) return null
		return tileGrid[x + y * width]
	}
}
