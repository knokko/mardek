package mardek.importer.area

import mardek.assets.area.*
import java.lang.Integer.parseInt
import java.util.*
import kotlin.streams.toList

fun main() {
	val parsingArea1 = parseArea1("aeropolis_N")
	val parsedArea = parseArea2(parsingArea1)
	println(parsedArea)
}

fun parseArea(areaName: String) = parseArea2(parseArea1(areaName))

private fun parseArea2(parsing: ParsingArea1): ParsedArea {
	val rawName = parseFlashString(parsing.variableAssignments["area"]!!, "raw area name")!!
	val displayName = parseFlashString(parsing.variableAssignments["areaname"]!!, "area display name")!!

	val musicTrack = parseFlashString(parsing.variableAssignments["musicTrack"]!!, "music track")

	val areaSetup = parsing.functionCalls.filter { it.first == "AreaSetup" }.map { it.second }
	parseAssert(areaSetup.size == 1, "Expected exactly 1 AreaSetup call, but found ${parsing.functionCalls}")
	val areaSetupMap = parseAreaSetup(areaSetup[0])
	val flags = parseAreaFlags(areaSetupMap)
	val dreamType = AreaDreamType.entries.find { it.code == (areaSetupMap["DREAM"] ?: "") }!!
	val chestType = AreaChestType.entries.find { it.code == parseInt(areaSetupMap["LOOT"] ?: "0") }!!
	val snowType = AreaSnowType.entries.find { it.code == parseInt(areaSetupMap["SNOW"] ?: "0") }!!

	// TODO Test proper ambience
	val rawDungeon = parsing.variableAssignments["dungeon"]
	val dungeon = if (rawDungeon != null) parseFlashString(rawDungeon, "dungeon")!! else null

	val rawAmbience = parsing.variableAssignments["ambience"]
	val ambience = if (rawAmbience != null) parseFlashString(rawAmbience, "ambience") else null

	var encyclopediaName: String? = null
	val encyclopediaAdd = parsing.functionCalls.filter { it.first == "EN_ADD" }.map { it.second }
	parseAssert(encyclopediaAdd.size <= 1, "Too many EN_ADDs: ${parsing.functionCalls}")

	if (encyclopediaAdd.isNotEmpty()) {
		val prefix = "\"Places\",\""
		parseAssert(encyclopediaAdd[0].startsWith(prefix), "Expected $encyclopediaAdd to start with $prefix")
		parseAssert(encyclopediaAdd[0].endsWith('"'), "Expected $encyclopediaAdd to end with a double quote")
		encyclopediaName = encyclopediaAdd[0].substring(prefix.length, encyclopediaAdd[0].length - 1)
	}

	val randomBattles = parseRandomBattle(parsing)
	val (width, height, tileGrid) = parseAreaMap(parsing.variableAssignments["map"]!!)

	return ParsedArea(
		rawName = rawName,
		displayName = displayName,
		tilesheetName = parseFlashString(parsing.variableAssignments["tileset"]!!, "tileset name")!!,
		// TODO Test this
		width = width,
		height = height,
		tileGrid = tileGrid,
		randomBattles = randomBattles,
		musicTrack = musicTrack,
		dungeon = dungeon,
		ambience = ambience,
		flags = flags,
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
		val pairs = rawPairs.map { {
			val rawSplit = it.split(":")
			Pair(rawSplit[0], rawSplit[1])
		} }

		// TODO Use the color packer
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
				state = when (state) {
					ParseState.Initial -> ParseState.Equals
					ParseState.BeforeValue -> ParseState.Value
					else -> throw AreaParseException("Unexpected whitespace at state $state at depth 0")
				}
				continue
			}
			if (character == '('.code && !state.isDeep) {
				parseAssert(state == ParseState.Initial, "Unexpected ( at state $state at depth 0")
				state = ParseState.Parameters
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

		val memory = when (state) {
			ParseState.Initial -> memory1
			ParseState.Value -> memory2
			ParseState.Parameters -> memory2
			else -> throw AreaParseException("Unexpected state $state")
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


