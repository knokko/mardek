package mardek.importer.area

import mardek.assets.area.*
import mardek.assets.battle.BattleAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.sprite.ArrowSprite
import mardek.assets.sprite.DirectionalSprites
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.resourcesFolder
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

fun importAreaAssets(inventoryAssets: InventoryAssets, battleAssets: BattleAssets, assets: AreaAssets = AreaAssets()): AreaAssets {
	importAreaBattleAssets(battleAssets, assets)

	val rawArrowSprites = importObjectSprites("trans_arrows")
	assets.arrowSprites.add(ArrowSprite("N", rawArrowSprites.frames[0]))
	assets.arrowSprites.add(ArrowSprite("E", rawArrowSprites.frames[1]))
	assets.arrowSprites.add(ArrowSprite("S", rawArrowSprites.frames[2]))
	assets.arrowSprites.add(ArrowSprite("W", rawArrowSprites.frames[3]))

	val closedChestSprites = importObjectSprites("chests", offsetY = 0, height = 16)
	val openedChestSprites = importObjectSprites("chests", offsetY = 16, height = 16)
	for (chestID in 0 until 6) {
		assets.chestSprites.add(ChestSprite(
			flashID = chestID,
			baseSprite = closedChestSprites.frames[chestID],
			openedSprite = openedChestSprites.frames[chestID]
		))
	}

	val areaFolder = File("$resourcesFolder/area")
	val charactersFolder = File("$areaFolder/sheets/character")
	for (characterSprite in charactersFolder.listFiles()!!) {
		val name = characterSprite.name
		if (!name.endsWith(".png")) throw AreaParseException("Unexpected sprite $characterSprite")

		val sheetImage = ImageIO.read(characterSprite)
		val numSprites = sheetImage.width / 16

		val sheet = DirectionalSprites(name.substring(0, name.length - 4), (0 until numSprites).map {
			compressKimSprite1(sheetImage.getSubimage(it * 16, 0, 16, sheetImage.height))
		}.toTypedArray())
		assets.characterSprites.add(sheet)
	}

	val parsedAreas = ArrayList<ParsedArea>()
	val parsedTilesheets = ArrayList<ParsedTilesheet>()
	val transitions = ArrayList<Pair<TransitionDestination, String>>()
	for (areaName in enumerateAreas(areaFolder)) {
		parsedAreas.add(parseArea(assets, areaName, parsedTilesheets, transitions, inventoryAssets, battleAssets))
	}

	val tileMapping = HashMap<ParsedTile, Tile>()

	for (parsed in parsedTilesheets) {
		val tilesheet = Tilesheet(parsed.name)
		tilesheet.waterSprites.addAll(parsed.waterSprites.map(::compressKimSprite1))
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
					sprites = ArrayList(parsedTile.sprites.map(::compressKimSprite1)),
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
			chests = ArrayList(parsedArea.chests),
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
