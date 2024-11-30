package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.assets.area.*
import mardek.assets.area.objects.AreaDecoration
import mardek.importer.util.ActionScriptCode
import mardek.importer.util.parseActionScriptResource
import java.io.File
import java.lang.Integer.parseInt

fun main() {
	val parsingArea1 = parseArea1("aeropolis_N")
	val parsedArea = parseArea2(parsingArea1)
	println(parsedArea)
}

fun enumerateAreas() = File("src/main/resources/mardek/importer/area/data").list().map {
	if (!it.endsWith(".txt")) throw java.lang.RuntimeException("Unexpected file $it")
	it.substring(0, it.length - 4)
}

fun parseArea(areaName: String) = parseArea2(parseArea1(areaName))

private fun parseArea2(areaCode: ActionScriptCode): ParsedArea {
	val areaSetup = areaCode.functionCalls.filter { it.first == "AreaSetup" }.map { it.second }
	parseAssert(areaSetup.size == 1, "Expected exactly 1 AreaSetup call, but found ${areaCode.functionCalls}")
	val areaSetupMap = parseAreaSetup(areaSetup[0])
	val properties = parseAreaProperties(areaCode, areaSetupMap)
	val flags = parseAreaFlags(areaSetupMap)

	val randomBattles = parseRandomBattle(areaCode)
	val (width, height, tileGrid) = parseAreaMap(areaCode.variableAssignments["map"]!!)

	val tilesheetName = parseFlashString(areaCode.variableAssignments["tileset"]!!, "tileset name")!!

	val tilesheet = parseTilesheet(tilesheetName)
	val extraDecorations = mutableListOf<AreaDecoration>()
	for (y in 0 until height) {
		for (x in 0 until width) {
			val tile = tilesheet.tiles[tileGrid[x + y * width]]!!
			if (tile.hexObjectColor != rgb(0, 0, 0)) {
				val hexObject = HexObject.map[tile.hexObjectColor]
					?: throw RuntimeException("unexpected hex color ${ColorPacker.toString(tile.hexObjectColor)}")
				extraDecorations.add(AreaDecoration(
					x = x, y = y, spritesheetName = hexObject.sheetName,
					spritesheetOffsetY = hexObject.height * hexObject.sheetRow,
					spriteHeight = hexObject.height, light = hexObject.light,
					timePerFrame = 50 * hexObject.timePerFrame,
					rawConversation = null, conversationName = null
				))
			}
		}
	}

	return ParsedArea(
		tilesheetName = tilesheetName,
		width = width,
		height = height,
		tileGrid = tileGrid,
		objects = parseAreaObjects(areaCode.variableAssignments["A_sprites"]!!, extraDecorations),
		randomBattles = randomBattles,
		properties = properties,
		flags = flags,
	)
}

fun parseAreaProperties(areaCode: ActionScriptCode, areaSetupMap: Map<String, String>): AreaProperties {
	val rawName = parseFlashString(areaCode.variableAssignments["area"]!!, "raw area name")!!
	val displayName = parseFlashString(areaCode.variableAssignments["areaname"]!!, "area display name")

	val rawMusicTrack = areaCode.variableAssignments["musicTrack"]
	var musicTrack = if (rawMusicTrack != null) parseFlashString(rawMusicTrack, "music track") else null
	if (musicTrack == "none") musicTrack = null
	val dreamType = AreaDreamType.entries.find { it.code == (areaSetupMap["DREAM"] ?: "") }!!
	val chestType = AreaChestType.entries.find { it.code == parseInt(areaSetupMap["LOOT"] ?: "0") }!!
	val snowType = AreaSnowType.entries.find { it.code == parseInt(areaSetupMap["SNOW"] ?: "0") }!!

	val rawDungeon = areaCode.variableAssignments["dungeon"]
	val dungeon = if (rawDungeon != null && rawDungeon != "null") parseFlashString(rawDungeon, "dungeon") else null

	val rawAmbience = areaCode.variableAssignments["ambience"]
	val ambience = if (rawAmbience != null) parseAmbience(rawAmbience) else null

	var encyclopediaName: String? = null
	val encyclopediaAdd = areaCode.functionCalls.filter { it.first == "EN_ADD" }.map { it.second }
	parseAssert(encyclopediaAdd.size <= 1, "Too many EN_ADDs: ${areaCode.functionCalls}")

	if (encyclopediaAdd.isNotEmpty()) {
		val prefix = "\"Places\",\""
		parseAssert(encyclopediaAdd[0].startsWith(prefix), "Expected $encyclopediaAdd to start with $prefix")
		parseAssert(encyclopediaAdd[0].endsWith('"'), "Expected $encyclopediaAdd to end with a double quote")
		encyclopediaName = encyclopediaAdd[0].substring(prefix.length, encyclopediaAdd[0].length - 1)
	}

	return AreaProperties(
		rawName = rawName,
		displayName = displayName ?: "unknown",
		ambience = ambience,
		musicTrack = musicTrack,
		dungeon = dungeon,
		encyclopediaName = encyclopediaName,
		dreamType = dreamType,
		chestType = chestType,
		snowType = snowType
	)
}

private fun parseAmbience(raw: String?): AreaAmbience? {
	if (raw == null || raw == "null") return null
	if (raw == "GenericExternalAmbience()") return AreaAmbience.GENERIC_EXTERNAL_AMBIENCE
	if (raw.startsWith("{") && raw.endsWith("}")) {
		val rawPairs = raw.substring(1, raw.length - 1).split(",")
		val pairs = rawPairs.map {
			val rawSplit = it.split(":")
			Pair(rawSplit[0], parseInt(rawSplit[1]))
		}
		val map = mutableMapOf(*pairs.toTypedArray())

		val colorA = rgba(map["ra"]!!, map["ga"]!!, map["ba"]!!, map["aa"]!!)
		val colorB = rgba(map["rb"]!!, map["gb"]!!, map["bb"]!!, map["ab"]!!)
		return AreaAmbience(colorA, colorB)
	}

	println("can't deal with ambience $raw")
	return null
}

private fun parseArea1(areaName: String) = parseActionScriptResource("mardek/importer/area/data/$areaName.txt")

class AreaParseException(message: String): RuntimeException(message)

internal fun parseAssert(assertion: Boolean, error: String) {
	if (!assertion) throw AreaParseException(error)
}

fun parseFlashString(flashString: String, description: String): String? {
	if (!flashString.startsWith('"') || !flashString.endsWith('"')) {
		println("Failed to parse $description $flashString")
		return null
	}
	return flashString.substring(1, flashString.length - 1).replace("\\'", "'")
}

fun parseOptionalFlashString(flashString: String?, description: String): String? {
	if (flashString == null || flashString == "null") return null
	return parseFlashString(flashString, description)
}
