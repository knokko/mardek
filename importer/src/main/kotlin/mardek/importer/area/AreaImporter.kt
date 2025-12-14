package mardek.importer.area

import mardek.content.Content
import mardek.content.area.*
import mardek.content.sprite.ArrowSprite
import mardek.content.sprite.DirectionalSprites
import mardek.importer.actions.HardcodedActions
import mardek.importer.story.expressions.HardcodedExpressions
import mardek.importer.util.compressKimSprite3
import mardek.importer.util.resourcesFolder
import mardek.importer.world.hardcodeWorldMap
import java.io.File
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

private val areaFolder = File("$resourcesFolder/area")

internal fun importAreaSprites(content: Content) {
	val rawArrowSprites = importObjectSprites("trans_arrows")
	content.areas.arrowSprites.add(ArrowSprite("N", rawArrowSprites.frames[0]))
	content.areas.arrowSprites.add(ArrowSprite("E", rawArrowSprites.frames[1]))
	content.areas.arrowSprites.add(ArrowSprite("S", rawArrowSprites.frames[2]))
	content.areas.arrowSprites.add(ArrowSprite("W", rawArrowSprites.frames[3]))

	val closedChestSprites = importObjectSprites("chests", offsetY = 0, height = 16)
	val openedChestSprites = importObjectSprites("chests", offsetY = 16, height = 16)
	for (chestID in 0 until 6) {
		content.areas.chestSprites.add(ChestSprite(
			flashID = chestID,
			baseSprite = closedChestSprites.frames[chestID],
			openedSprite = openedChestSprites.frames[chestID]
		))
	}

	val charactersFolder = File("$areaFolder/sheets/character")
	for (characterSprite in charactersFolder.listFiles()!!) {
		val name = characterSprite.name
		if (!name.endsWith(".png")) throw AreaParseException("Unexpected sprite $characterSprite")

		val sheetImage = ImageIO.read(characterSprite)
		val numSprites = sheetImage.width / 16

		val sheet = DirectionalSprites(name.dropLast(4), (0 until numSprites).map {
			compressKimSprite3(sheetImage.getSubimage(it * 16, 0, 16, sheetImage.height))
		}.toTypedArray())
		content.areas.characterSprites.add(sheet)
	}
}

internal fun importAreaContent(content: Content): HardcodedActions {
	val hardcodedActions = HardcodedActions()
	hardcodedActions.hardcodeActionSequences(content)

	val hardcodedExpressions = HardcodedExpressions()
	hardcodedExpressions.hardcodeTimelineExpressions(content)

	val parsedAreas = ArrayList<ParsedArea>()
	val parsedTilesheets = ArrayList<ParsedTilesheet>()
	val transitions = ArrayList<Pair<TransitionDestination, String>>()
	for (areaName in enumerateAreas(areaFolder)) {
		val context = AreaEntityParseContext(content, areaName, hardcodedActions, hardcodedExpressions, transitions)
		parsedAreas.add(parseArea(context, parsedTilesheets))
	}

	val tileMapping = HashMap<ParsedTile, Tile>()

	for (parsed in parsedTilesheets) {
		val tilesheet = Tilesheet(parsed.name)
		tilesheet.waterSprites.addAll(parsed.waterSprites.map(::compressKimSprite3))
		content.areas.tilesheets.add(tilesheet)

		val usedTiles = HashSet<Int>()
		for (area in parsedAreas.filter { it.tilesheet == parsed }) {
			for (y in 0 until area.height) {
				for (x in 0 until area.width) usedTiles.add(area.getTileId(x, y)!!)
			}
		}

		for (tileID in usedTiles) {
			val parsedTile = parsed.tiles[tileID]!!
			val tile = Tile(
					sprites = ArrayList(parsedTile.sprites.map(::compressKimSprite3)),
					canWalkOn = parsedTile.canWalkOn,
					waterType = parsedTile.waterType
			)
			tilesheet.tiles.add(tile)
			tileMapping[parsedTile] = tile
		}
	}

	for (parsedArea in parsedAreas) {
		val tilesheet = content.areas.tilesheets.find { it.name == parsedArea.tilesheet.name }!!
		val tileGrid = Array(parsedArea.width * parsedArea.height) { index ->
			tileMapping[parsedArea.tilesheet.tiles[parsedArea.tileGrid[index]]!!]!!
		}
		content.areas.areas.add(Area(
			width = parsedArea.width,
			height = parsedArea.height,
			tilesheet = tilesheet,
			tileGrid = tileGrid,
			objects = parsedArea.objects,
			chests = ArrayList(parsedArea.chests),
			randomBattles = parsedArea.randomBattles,
			flags = parsedArea.flags,
			properties = parsedArea.properties,
			id = parsedArea.id,
		))
	}

	hardcodeWorldMap(content)

	for ((transition, destination) in transitions) {
		if (destination == "WORLDMAP") {
			transition.worldMap = content.worldMaps[0]
			continue
		}
		if (destination == "nowhere") continue
		transition.area = content.areas.areas.find {
			it.properties.rawName.equals(destination, ignoreCase = true)
		}!!
	}

	return hardcodedActions
}

internal fun enumerateAreas(areaFolder: File) = File("$areaFolder/data").list()!!.map {
	if (!it.endsWith(".txt")) throw java.lang.RuntimeException("Unexpected file $it")
	it.substring(0, it.length - 4)
}
