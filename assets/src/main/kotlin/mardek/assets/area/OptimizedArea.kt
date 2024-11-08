package mardek.assets.area

import mardek.assets.area.objects.AreaObjects

class OptimizedArea(
	val width: Int,
	val height: Int,
	private val lowTileGrid: IntArray,
	private val highTileGrid: IntArray,
	val objects: AreaObjects,
	// TODO Loot
	val randomBattles: RandomAreaBattles?,
	val flags: AreaFlags,
	val properties: AreaProperties,
) {
}
