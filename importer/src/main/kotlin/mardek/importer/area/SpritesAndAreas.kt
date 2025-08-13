package mardek.importer.area

import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.area.*
import mardek.content.sprite.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import kotlin.collections.ArrayList

class SpritesAndAreas {

	private val kimSprites = ArrayList<KimSprite>()
	private val storedAreas = ArrayList<StoredAreaRenderData>()

	private var kimOffset = 0

	fun registerSprite(sprite: KimSprite) {
		if (sprite.index != -1) throw Error("Encountered tile twice")
		sprite.index = kimOffset
		kimOffset += sprite.data!!.size
		kimSprites.add(sprite)
	}

	fun register(assets: AreaContent) {
		for (tilesheet in assets.tilesheets) {
			for (sprite in tilesheet.waterSprites) registerSprite(sprite)
			for (tile in tilesheet.tiles) {
				if (tile.sprites[0].index != -1) throw Error("Shouldn't happen")
				for (sprite in tile.sprites) registerSprite(sprite)
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

		for (arrow in assets.arrowSprites) registerSprite(arrow.sprite)

		for (area in assets.areas) register(area)
	}

	private fun register(area: Area) {
		val tileSpriteMap = mutableMapOf<KimSprite, Int>()
		for (tile in area.tileGrid!!) {
			for (sprite in tile.sprites) {
				if (!tileSpriteMap.containsKey(sprite)) tileSpriteMap[sprite] = tileSpriteMap.size
			}
		}

		fun water(index: Int) = area.tilesheet.waterSprites[index]
		val renderData = StoredAreaRenderData(
			areaID = area.id,
			width = area.width,
			tileSprites = tileSpriteMap.entries.sortedBy { it.value }.map { it.key }.toTypedArray(),
			waterSprites = arrayOf(water(0), water(2), water(3), water(4), water(1)),
			indirectLowTiles = IntArray(area.width * area.height),
			indirectHigherTiles = Array(2 * area.width * area.height) { null },
			indirectWater = IntArray(area.width * area.height)
		)
		storedAreas.add(renderData)

		val canWalkGrid = BooleanArray(area.width * area.height)

		for (y in 0 until area.height) {
			for (x in 0 until area.width) {
				val tile = area.getTile(x, y)

				canWalkGrid[x + y * area.width] = tile.canWalkOn
				renderData.indirectLowTiles[x + y * area.width] = tileSpriteMap[tile.sprites.last()]!!

				renderData.indirectWater[x + y * area.width] = when (tile.waterType) {
					WaterType.None -> 0
					WaterType.Water -> 1
					WaterType.Lava -> 2
					WaterType.Waterfall -> 3
					else -> throw Error("Unexpected water type ${tile.waterType}")
				}

				if (y < area.height - 1) {
					val midTile = area.getTile(x, y + 1)
					if (midTile.sprites.size > 1) {
						renderData.indirectHigherTiles[2 * (x + y * area.width)] = tileSpriteMap[midTile.sprites[midTile.sprites.size - 2]]
					}
				}

				if (y < area.height - 2) {
					val highTile = area.getTile(x, y + 2)
					if (highTile.sprites.size > 2) {
						renderData.indirectHigherTiles[1 + 2 * (x + y * area.width)] = tileSpriteMap[highTile.sprites[highTile.sprites.size - 3]]
					}
				}
			}
		}

		for (chest in area.chests) canWalkGrid[chest.x + area.width * chest.y] = false
		for (door in area.objects.doors) {
			if (door.x >= 0 && door.y >= 0 && door.x < area.width && door.y < area.height) {
				canWalkGrid[door.x + area.width * door.y] = false
			}
		}
		for (orb in area.objects.switchOrbs) canWalkGrid[orb.x + area.width * orb.y] = false

//		area.tileGrid = null
//		area.canWalkGrid = canWalkGrid
	}

	fun writeKimSprites(output: OutputStream) {
		val data = DataOutputStream(output)

		data.writeInt(kimOffset)

		for (sprite in kimSprites) {
			for (value in sprite.data!!) data.writeInt(value)
			sprite.data = null
		}
	}

	fun writeAreaOffsets(output: OutputStream, bitser: Bitser) {
		val data = DataOutputStream(output)

		data.writeInt(storedAreas.size)
		for (stored in storedAreas) {
			data.writeLong(stored.areaID.mostSignificantBits)
			data.writeLong(stored.areaID.leastSignificantBits)

			val byteOutput = ByteArrayOutputStream()
			val bitOutput = BitOutputStream(byteOutput)
			bitser.serialize(stored, bitOutput)
			bitOutput.finish()

			val compressed = byteOutput.toByteArray()
			data.writeInt(compressed.size)
			data.write(compressed)
		}
	}
}
