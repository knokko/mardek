package mardek.renderer.area

import mardek.content.area.StoredAreaRenderData
import mardek.content.area.WaterType
import mardek.content.sprite.KimSprite

class MemoryAreaRenderData(
	private val width: Int,
	val tileSprites: Array<KimSprite>,
	val waterSprites: Array<KimSprite>,
	private val packedTiles: IntArray
) {

	private fun index(x: Int, y: Int) = x + y * width

	fun getWaterType(x: Int, y: Int) = when (getWaterSpriteIndex(x, y)) {
		0 -> WaterType.None
		1 -> WaterType.Water
		2 -> WaterType.Lava
		3 -> WaterType.Waterfall
		else -> throw Error("Invalid tile")
	}

	fun getWaterSpriteIndex(x: Int, y: Int) = if (x < 0 || y < 0 || x >= width || y >= packedTiles.size / width) 0
	else packedTiles[index(x, y)] and 3

	fun getTileSpriteIndex(x: Int, y: Int, height: Int) = (packedTiles[index(x, y)] shr (2 + 10 * height)) and 1023

	companion object {
		fun pack(compact: StoredAreaRenderData): MemoryAreaRenderData {
			val packedTiles = IntArray(compact.indirectWater.size)
			for (index in packedTiles.indices) {
				var packed = compact.indirectWater[index] or (compact.indirectLowTiles[index] shl 2)
				val midIndex = compact.indirectHigherTiles[2 * index] ?: 1023
				val highIndex = compact.indirectHigherTiles[2 * index + 1] ?: 1023
				packed = packed or (midIndex shl 12)
				packed = packed or (highIndex shl 22)
				packedTiles[index] = packed
			}
			return MemoryAreaRenderData(
				width = compact.width,
				tileSprites = compact.tileSprites,
				waterSprites = compact.waterSprites,
				packedTiles = packedTiles
			)
		}
	}
}
