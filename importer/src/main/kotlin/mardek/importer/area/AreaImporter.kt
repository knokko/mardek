package mardek.importer.area

import mardek.assets.area.*
import mardek.assets.sprite.DirectionalSprites
import mardek.importer.util.compressSprite
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

fun importAreaAssets(areaFolder: File): AreaAssets {
	val assets = AreaAssets()

	val charactersFolder = File("$areaFolder/sheets/character")
	for (characterSprite in charactersFolder.listFiles()!!) {
		val name = characterSprite.name
		if (!name.endsWith(".png")) throw AreaParseException("Unexpected sprite $characterSprite")

		val sheetImage = ImageIO.read(characterSprite)
		val numSprites = sheetImage.width / 16

		val sheet = DirectionalSprites(name.substring(0, name.length - 4), (0 until numSprites).map {
			compressSprite(sheetImage.getSubimage(it * 16, 0, 16, sheetImage.height))
		}.toTypedArray())
		assets.characterSprites.add(sheet)
	}

	val parsedAreas = ArrayList<ParsedArea>()
	val parsedTilesheets = ArrayList<ParsedTilesheet>()
	val transitions = ArrayList<Pair<TransitionDestination, String>>()
	for (areaName in enumerateAreas(areaFolder)) {
		parsedAreas.add(parseArea(assets, areaName, parsedTilesheets, transitions))
	}

	val tileMapping = HashMap<ParsedTile, Tile>()

	for (parsed in parsedTilesheets) {
		val tilesheet = Tilesheet(parsed.name)
		tilesheet.waterSprites.addAll(parsed.waterSprites.map(::compressSprite))
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
					sprites = ArrayList(parsedTile.sprites.map(::compressSprite)),
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
		if (destination == "WORLDMAP") continue // TODO Handle this
		if (destination == "nowhere") continue
		transition.area = assets.areas.find { it.properties.rawName.lowercase(Locale.ROOT) == destination.lowercase(Locale.ROOT) }!!
	}

	return assets
}

internal fun enumerateAreas(areaFolder: File) = File("$areaFolder/data").list()!!.map {
	if (!it.endsWith(".txt")) throw java.lang.RuntimeException("Unexpected file $it")
	it.substring(0, it.length - 4)
}
