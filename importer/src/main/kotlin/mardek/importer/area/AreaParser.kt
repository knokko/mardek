package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.assets.area.*
import mardek.assets.area.objects.AreaDecoration
import java.io.File
import java.lang.Integer.parseInt
import java.util.*
import kotlin.streams.toList

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

private fun parseArea2(parsing: ParsingArea1): ParsedArea {
	val areaSetup = parsing.functionCalls.filter { it.first == "AreaSetup" }.map { it.second }
	parseAssert(areaSetup.size == 1, "Expected exactly 1 AreaSetup call, but found ${parsing.functionCalls}")
	val areaSetupMap = parseAreaSetup(areaSetup[0])
	val properties = parseAreaProperties(parsing, areaSetupMap)
	val flags = parseAreaFlags(areaSetupMap)

	val randomBattles = parseRandomBattle(parsing)
	val (width, height, tileGrid) = parseAreaMap(parsing.variableAssignments["map"]!!)

	val tilesheetName = parseFlashString(parsing.variableAssignments["tileset"]!!, "tileset name")!!

	val tilesheet = parseTilesheet(tilesheetName)
	val extraDecorations = mutableListOf<AreaDecoration>()
	for (y in 0 until height) {
		for (x in 0 until width) {
			val tile = tilesheet.tiles[tileGrid[x + y * width]]!!
			if (tile.hexObjectColor != rgb(0, 0, 0)) {
				val hexObject = HexObject.map[tile.hexObjectColor]
					?: throw RuntimeException("unexpected hex color ${ColorPacker.toString(tile.hexObjectColor)}")
				extraDecorations.add(
					AreaDecoration(
					x = x, y = y, spritesheetName = hexObject.sheetName,
					spritesheetOffsetY = hexObject.height * hexObject.sheetRow,
					spriteHeight = hexObject.height, light = hexObject.light,
					rawConversation = null
				)
				)
			}
		}
	}

	return ParsedArea(
		tilesheetName = tilesheetName,
		width = width,
		height = height,
		tileGrid = tileGrid,
		objects = parseAreaObjects(parsing.variableAssignments["A_sprites"]!!, extraDecorations),
		randomBattles = randomBattles,
		properties = properties,
		flags = flags,
	)
}

fun parseAreaProperties(parsing: ParsingArea1, areaSetupMap: Map<String, String>): AreaProperties {
	val rawName = parseFlashString(parsing.variableAssignments["area"]!!, "raw area name")!!
	val displayName = parseFlashString(parsing.variableAssignments["areaname"]!!, "area display name")

	val rawMusicTrack = parsing.variableAssignments["musicTrack"]
	val musicTrack = if (rawMusicTrack != null) parseFlashString(rawMusicTrack, "music track") else null
	val dreamType = AreaDreamType.entries.find { it.code == (areaSetupMap["DREAM"] ?: "") }!!
	val chestType = AreaChestType.entries.find { it.code == parseInt(areaSetupMap["LOOT"] ?: "0") }!!
	val snowType = AreaSnowType.entries.find { it.code == parseInt(areaSetupMap["SNOW"] ?: "0") }!!

	val rawDungeon = parsing.variableAssignments["dungeon"]
	val dungeon = if (rawDungeon != null) parseFlashString(rawDungeon, "dungeon") else null

	val rawAmbience = parsing.variableAssignments["ambience"]
	val ambience = if (rawAmbience != null) parseAmbience(rawAmbience) else null

	var encyclopediaName: String? = null
	val encyclopediaAdd = parsing.functionCalls.filter { it.first == "EN_ADD" }.map { it.second }
	parseAssert(encyclopediaAdd.size <= 1, "Too many EN_ADDs: ${parsing.functionCalls}")

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

private fun parseArea1(areaName: String): ParsingArea1 {
	val scanner = Scanner(TileSlice::class.java.getResourceAsStream("data/$areaName.txt"))
	val lines = mutableListOf<String>()
	while (scanner.hasNextLine()) lines.add(scanner.nextLine())
	scanner.close()

	val content = lines.flatMap { it.codePoints().toList() }
	var depth = 0

	val variableAssignments = mutableMapOf<String, String>()
	val functionCalls = mutableListOf<Pair<String, String>>()

	val memory1 = StringBuilder()
	val memory2 = StringBuilder()
	var state = ParseState.Initial

	for (character in content) {
		if (depth == 0) {
			if (character == ' '.code && !state.isDeep) {
				val mem1 = memory1.toString()
				if (mem1 != "var" && mem1 != "else") {
					state = when (state) {
						ParseState.Initial -> ParseState.Equals
						ParseState.BeforeValue -> ParseState.Value
						ParseState.BeforeIfBody -> ParseState.BeforeIfBody
						else -> throw AreaParseException("Unexpected whitespace at state $state at depth 0")
					}
					continue
				}
			}
			if (character == '('.code && !state.isDeep) {
				parseAssert(state == ParseState.Initial, "Unexpected ( at state $state at depth 0")
				val isControl = when (memory1.toString().trim()) {
					"if" -> true
					"else if" -> true
					"while" -> true
					"for" -> true
					else -> false
				}
				state = if (isControl) {
					depth += 1
					ParseState.IfCondition
				} else ParseState.Parameters
				continue
			}
			if (character == '{'.code && (state == ParseState.BeforeIfBody ||
						(state == ParseState.Initial && memory1.toString().trim() == "else"))
			) {
				state = ParseState.InsideIfBody
				depth += 1
				continue
			}
			if (character == ')'.code && state == ParseState.Parameters) continue
			if (character == '='.code && state != ParseState.Value) {
				parseAssert(state == ParseState.Equals, "Unexpected = at state $state at depth 0")
				state = ParseState.BeforeValue
				continue
			}
			if (character == ';'.code) {
				when (state) {
					ParseState.Value -> variableAssignments[memory1.toString()] = memory2.toString()
					ParseState.Parameters -> functionCalls.add(Pair(memory1.toString(), memory2.toString()))
					ParseState.Initial -> parseAssert(memory1.startsWith("var "), "Unexpected ; at depth 0 after $memory1")
					else -> throw AreaParseException("Unexpected ; at depth 0 at state $state")
				}
				memory1.clear()
				memory2.clear()
				state = ParseState.Initial
				continue
			}
		}

		if (character == '['.code || character == '{'.code) depth += 1
		if (character == ']'.code || character == '}'.code) depth -= 1
		if (character == '('.code && state == ParseState.IfCondition) depth += 1
		if (character == ')'.code && state == ParseState.IfCondition) {
			depth -= 1
			if (depth == 0) state = ParseState.BeforeIfBody
		}
		if (character == '}'.code && state == ParseState.InsideIfBody && depth == 0) {
			state = ParseState.Initial
			memory1.clear()
			memory2.clear()
			continue
		}

		if (state == ParseState.BeforeIfBody) continue

		val memory = when (state) {
			ParseState.Initial -> memory1
			ParseState.Value -> memory2
			ParseState.Parameters -> memory2
			ParseState.IfCondition -> memory1
			ParseState.InsideIfBody -> memory2
			else -> throw AreaParseException("Unexpected state $state with memory $memory1 and $memory2")
		}
		memory.appendCodePoint(character)
	}

	parseAssert(depth == 0, "Expected to end at depth = 0, but ended up at depth = $depth")
	return ParsingArea1(variableAssignments, functionCalls)
}

private enum class ParseState(val isDeep: Boolean) {
	Initial(false),
	Equals(false),
	BeforeValue(false),
	Value(true),
	Parameters(true),
	IfCondition(true),
	BeforeIfBody(false),
	InsideIfBody(true),
}

class AreaParseException(message: String): RuntimeException(message)

internal fun parseAssert(assertion: Boolean, error: String) {
	if (!assertion) throw AreaParseException(error)
}

class ParsingArea1(val variableAssignments: Map<String, String>, val functionCalls: List<Pair<String, String>>)

fun parseFlashString(flashString: String, description: String): String? {
	if (!flashString.startsWith('"') || !flashString.endsWith('"')) {
		println("Failed to parse $description $flashString")
		return null
	}
	return flashString.substring(1, flashString.length - 1).replace("\\'", "'")
}


