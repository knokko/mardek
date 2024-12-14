package mardek.importer.area

import mardek.assets.area.*
import mardek.assets.sprite.*
import java.io.DataOutputStream
import java.io.OutputStream
import kotlin.collections.ArrayList

class AreaSprites {

	private val kimSprites = ArrayList<KimSprite>()
	private val highTileSprites = ArrayList<KimSprite>()
	private val tileGrids = ArrayList<IntArray>()

	private var kimOffset = 0
	private var highTileOffset = 0
	private var tileOffset = 0

	private fun registerSprite(sprite: KimSprite) {
		if (sprite.offset != -1) throw Error("Encountered tile twice")
		sprite.offset = kimOffset
		kimOffset += sprite.data!!.size
		kimSprites.add(sprite)
	}

	fun register(assets: AreaAssets) {
		for (tilesheet in assets.tilesheets) {
			for (sprite in tilesheet.waterSprites) registerSprite(sprite)
			for (tile in tilesheet.tiles) {
				if (tile.sprites[0].offset != -1) throw Error("Shouldn't happen")
				for ((sourceIndex, sprite) in tile.sprites.withIndex()) {
					if (sourceIndex == tile.sprites.size - 1) {
						registerSprite(sprite)
					} else {
						sprite.offset = highTileOffset
						highTileOffset += sprite.data!!.size
						highTileSprites.add(sprite)
					}
				}
			}
			tilesheet.tiles.clear()
		}

		for (color in assets.switchColors) {
			for (sprite in arrayOf(color.offSprite, color.onSprite, color.gateSprite, color.platformSprite)) {
				registerSprite(sprite)
			}
		}

		for (sheet in assets.characterSprites) {
			for (sprite in sheet.sprites) registerSprite(sprite)
		}

		for (sheet in assets.objectSprites) {
			for (sprite in sheet.frames) registerSprite(sprite)
		}

		for (area in assets.areas) register(area)
	}

	private fun register(area: Area) {
		val lowTilesOffset = tileOffset
		tileOffset += area.width * area.height
		val lowTiles = IntArray(area.width * area.height)
		tileGrids.add(lowTiles)

		val highTilesOffset = tileOffset
		tileOffset += area.width * area.height
		val highTiles = IntArray(area.width * area.height)
		tileGrids.add(highTiles)

		val canWalkGrid = BooleanArray(area.width * area.height)

		for (y in 0 until area.height) {
			for (x in 0 until area.width) {
				val tile = area.getTile(x, y)

				canWalkGrid[x + y * area.width] = tile.canWalkOn

				var lowTile = tile.sprites.last().offset
				if (lowTile >= 1 shl 24) throw UnsupportedOperationException("Tile sprite index too large: $lowTile")
				if (y > 0 && area.getTile(x, y - 1).waterType != WaterType.None) {
					lowTile = lowTile or (1 shl 30)
				}

				lowTiles[x + y * area.width] = lowTile or (tile.waterType.ordinal shl 24)

				var midPart = -1
				if (y < area.height - 1) {
					val midTile = area.getTile(x, y + 1)
					if (midTile.sprites.size > 1) {
						midPart = midTile.sprites[midTile.sprites.size - 2].offset
						if (midPart < 0 || midPart > UShort.MAX_VALUE.toInt()) {
							throw UnsupportedOperationException("Uh ooh: mid tile sprite offset is $midPart")
						}
					}
				}

				var highPart = -1
				if (y < area.height - 2) {
					val highTile = area.getTile(x, y + 2)
					if (highTile.sprites.size > 2) {
						highPart = highTile.sprites[0].offset
						if (highPart < 0 || highPart > UShort.MAX_VALUE.toInt()) {
							throw UnsupportedOperationException("Uh ooh: high tile sprite offset is $highPart")
						}
					}
				}

				highTiles[x + y * area.width] = (midPart and 0xFFFF) or ((highPart and 0xFFFF) shl 16)
			}
		}

		area.renderLowTilesOffset = lowTilesOffset
		area.renderHighTilesOffset = highTilesOffset
		area.tileGrid = null
		area.canWalkGrid = canWalkGrid
	}

	fun writeRenderData(output: OutputStream) {
		val data = DataOutputStream(output)

		data.writeInt(kimOffset)
		data.writeInt(highTileOffset)
		data.writeInt(tileOffset)

		for (sprite in kimSprites) {
			for (value in sprite.data!!) data.writeInt(value)
			sprite.data = null
		}

		for (sprite in highTileSprites) {
			for (value in sprite.data!!) data.writeInt(value)
			sprite.data = null
		}

		for (tileGrid in tileGrids) {
			for (value in tileGrid) data.writeInt(value)
		}

		data.flush()
	}
}
