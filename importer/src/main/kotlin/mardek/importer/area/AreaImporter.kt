package mardek.importer.area

import mardek.assets.area.*
import mardek.assets.sprite.KimSprite
import mardek.importer.util.compressSprite
import java.io.File

fun importAllAreas() {
	val assets = AreaAssets()
	val parsedAreas = ArrayList<ParsedArea>()
	val parsedTilesheets = ArrayList<ParsedTilesheet>()
	val transitions = ArrayList<Pair<TransitionDestination, String>>()
	for (areaName in enumerateAreas()) {
		parsedAreas.add(parseArea(assets, areaName, parsedTilesheets, transitions))
	}

	val tileMapping = HashMap<ParsedTile, Tile>()

	for (parsed in parsedTilesheets) {
		val tilesheet = Tilesheet(parsed.name)
		tilesheet.waterSprites.addAll(parsed.waterSprites.map { KimSprite(compressSprite(it)) })
		assets.tilesheets.add(tilesheet)

		val usedTiles = HashSet<Int>()
		for (area in parsedAreas.filter { it.tilesheet == parsed }) {
			for (y in 0 until area.height) {
				for (x in 0 until area.width) usedTiles.add(area.getTileId(x, y)!!)
			}
		}

		for (tileID in usedTiles) {
			val parsedTile = parsed.tiles[tileID]!!
			val tile = Tile(
					sprites = parsedTile.sprites.map { KimSprite(compressSprite(it)) },
					canWalkOn = parsedTile.canWalkOn,
					waterType = parsedTile.waterType
			)
			tilesheet.tiles.add(tile)
			tileMapping[parsedTile] = tile
		}
	}

	for (parsedArea in parsedAreas) {
		val tilesheet = assets.tilesheets.find { it.name == parsedArea.tilesheet.name }!!
		val tileGrid = Array(parsedArea.width * parsedArea.height) { index ->
			tileMapping[parsedArea.tilesheet.tiles[parsedArea.tileGrid[index]]!!]!!
		}
		assets.areas.add(Area(
				width = parsedArea.width,
				height = parsedArea.height,
				tilesheet = tilesheet,
				tileGrid = tileGrid,
				objects = parsedArea.objects,
				randomBattles = parsedArea.randomBattles,
				flags = parsedArea.flags,
				properties = parsedArea.properties,
		))
	}

	for ((transition, destination) in transitions) {
		transition.area = assets.areas.find { it.properties.rawName == destination }!!
	}
}

internal fun enumerateAreas() = File("src/main/resources/mardek/importer/area/data").list().map {
	if (!it.endsWith(".txt")) throw java.lang.RuntimeException("Unexpected file $it")
	it.substring(0, it.length - 4)
}