package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.Content
import mardek.content.animation.ColorTransform
import mardek.content.area.*
import mardek.content.area.objects.AreaDecoration
import mardek.content.story.*
import mardek.importer.util.ActionScriptCode
import mardek.importer.util.parseActionScriptResource
import java.lang.Integer.parseInt
import java.util.UUID

internal fun parseArea(
	context: AreaEntityParseContext,
	tilesheets: MutableList<ParsedTilesheet>,
) = parseArea2(context, parseArea1(context.areaName), tilesheets)

private fun parseArea2(
	context: AreaEntityParseContext,
	areaCode: ActionScriptCode,
	tilesheets: MutableList<ParsedTilesheet>,
): ParsedArea {
	val areaSetup = areaCode.functionCalls.filter { it.first == "AreaSetup" }.map { it.second }
	parseAssert(areaSetup.size == 1, "Expected exactly 1 AreaSetup call, but found ${areaCode.functionCalls}")
	val areaSetupMap = parseAreaSetup(areaSetup[0])
	val properties = parseAreaProperties(context.content, areaCode, areaSetupMap)
	val flags = parseAreaFlags(areaSetupMap)

	val randomBattles = parseRandomBattle(areaCode, context.content)
	var (width, height, tileGrid) = parseAreaMap(areaCode.variableAssignments["map"]!!)

	val tilesheetName = parseFlashString(areaCode.variableAssignments["tileset"]!!, "tileset name")!!
	var tilesheet = tilesheets.find { it.name == tilesheetName }
	if (tilesheet == null) {
		tilesheet = parseTilesheet(tilesheetName)
		tilesheets.add(tilesheet)
	}

	val extraDecorations = mutableListOf<AreaDecoration>()
	for (y in 0 until height) {
		for (x in 0 until width) {
			val tile = tilesheet.tiles[tileGrid[x + y * width]]!!
			if (tile.hexObjectColor != rgb(0, 0, 0)) {
				val hexObject = HexObject.map[tile.hexObjectColor]
					?: throw RuntimeException("unexpected hex color ${ColorPacker.toString(tile.hexObjectColor)}")

				val spriteID = "${hexObject.sheetName}(${hexObject.sheetRow}, ${hexObject.height})"
				var sprites = context.content.areas.objectSprites.find { it.flashName == spriteID }
				if (sprites == null) {
					sprites = importObjectSprites(
							hexObject.sheetName, offsetY = hexObject.height * hexObject.sheetRow, height = hexObject.height
					)
					sprites.flashName = spriteID
					context.content.areas.objectSprites.add(sprites)
				}

				extraDecorations.add(AreaDecoration(
					x = x, y = y, sprites = sprites, canWalkThrough = true, light = hexObject.light,
					timePerFrame = 50 * hexObject.timePerFrame,
					rawConversation = null, conversationName = null, actionSequence = null, signType = null,
				))
			}
		}
	}

	val rawAreaLoot = areaCode.variableAssignments["areaLoot"]
	val rawChestID = parseInt(areaSetupMap["LOOT"] ?: "0")
	val chestType = context.content.areas.chestSprites.find { it.flashID == rawChestID }!!

	var minTileX = 0
	var minTileY = 0

	if (areaCode.variableAssignments.containsKey("leftBorderTile")) {
		val leftTile = parseInt(areaCode.variableAssignments["leftBorderTile"])
		val oldWidth = width
		val oldTileGrid = tileGrid
		width += 1
		minTileX = -1
		tileGrid = IntArray(width * height)
		for (y in 0 until height) {
			tileGrid[y * width] = leftTile
			oldTileGrid.copyInto(
				destination = tileGrid, destinationOffset = 1 + y * width,
				startIndex = y * oldWidth, endIndex = (1 + y) * oldWidth
			)
		}
	}

	if (areaCode.variableAssignments.containsKey("rightBorderTile")) {
		val rightTile = parseInt(areaCode.variableAssignments["rightBorderTile"])
		val oldWidth = width
		val oldTileGrid = tileGrid
		width += 1
		tileGrid = IntArray(width * height)
		for (y in 0 until height) {
			tileGrid[y * width + oldWidth] = rightTile
			oldTileGrid.copyInto(
				destination = tileGrid, destinationOffset = y * width,
				startIndex = y * oldWidth, endIndex = (1 + y) * oldWidth
			)
		}
	}

	if (areaCode.variableAssignments.containsKey("upBorderTile")) {
		val upperTile = parseInt(areaCode.variableAssignments["upBorderTile"])
		height += 1
		minTileY = -1
		tileGrid = IntArray(width) { upperTile } + tileGrid
	}

	if (areaCode.variableAssignments.containsKey("lowBorderTile")) {
		val lowerTile = parseInt(areaCode.variableAssignments["lowBorderTile"])
		height += 1
		tileGrid += IntArray(width) { lowerTile }
	}

	return ParsedArea(
		tilesheet = tilesheet,
		width = width,
		height = height,
		minTileX = minTileX,
		minTileY = minTileY,
		tileGrid = tileGrid,
		objects = parseAreaObjects(context, areaCode.variableAssignments["A_sprites"]!!, extraDecorations),
		chests = if (rawAreaLoot != null) {
			parseAreaChests(context.content, rawAreaLoot, chestType)
		} else ArrayList(0),
		randomBattles = randomBattles,
		properties = properties,
		flags = flags,
		id = UUID.fromString(areaCode.variableAssignments["uuid"]!!),
	)
}

internal fun parseAreaProperties(content: Content, areaCode: ActionScriptCode, areaSetupMap: Map<String, String>): AreaProperties {
	val rawName = parseFlashString(areaCode.variableAssignments["area"]!!, "raw area name")!!
	val displayName = parseFlashString(areaCode.variableAssignments["areaname"]!!, "area display name")

	val rawMusicTrack = areaCode.variableAssignments["musicTrack"]
	val musicTrack = if (rawMusicTrack != null) parseMusicTrack(content, rawMusicTrack)
	else throw RuntimeException("Missing music track of $rawName")
	val dreamType = AreaDreamType.entries.find { it.code == (areaSetupMap["DREAM"] ?: "") }!!
	val snowType = AreaSnowType.entries.find { it.code == parseInt(areaSetupMap["SNOW"] ?: "0") }!!

	val rawDungeon = areaCode.variableAssignments["dungeon"]
	val dungeon = if (rawDungeon != null && rawDungeon != "null") parseFlashString(rawDungeon, "dungeon") else null

	val ambience = parseAmbience(content, areaCode.variableAssignments["ambience"])

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
		snowType = snowType,
	)
}

@Suppress("UNCHECKED_CAST")
private fun parseMusicTrack(content: Content, raw: String): TimelineExpression<String?> {
	if (raw == "\"none\"") return ConstantTimelineExpression(TimelineOptionalStringValue(null))
	if (raw.startsWith('"') && raw.endsWith('"')) {
		return ConstantTimelineExpression(
			TimelineOptionalStringValue(raw.substring(1, raw.length - 1))
		)
	}

	fun timeOfDayMusic(dayMusic: String) = ExpressionOrDefaultTimelineExpression(
		GlobalTimelineExpression(content.story.globalExpressions.find {
			it.name == "TimeOfDayMusic"
		}!! as GlobalExpression<String?>),
		ConstantTimelineExpression(TimelineOptionalStringValue(dayMusic))
	)

	fun variableOrTimeOfDayMusic(variableName: String, defaultMusic: String) = ExpressionOrDefaultTimelineExpression(
		VariableTimelineExpression(content.story.customVariables.find {
			it.name == variableName
		}!! as CustomTimelineVariable<String?>),
		timeOfDayMusic(defaultMusic)
	)

	fun variableOrDefaultMusic(variableName: String, defaultMusic: String) = ExpressionOrDefaultTimelineExpression(
		VariableTimelineExpression(content.story.customVariables.find {
			it.name == variableName
		}!! as CustomTimelineVariable<String?>),
		ConstantTimelineExpression(TimelineOptionalStringValue(defaultMusic))
	)

	fun castleGoznorMusic(variableName: String) = ExpressionOrDefaultTimelineExpression(
		VariableTimelineExpression(content.story.customVariables.find {
			it.name == variableName
		}!! as CustomTimelineVariable<String?>),
		SwitchCaseTimelineExpression(
			input = ExpressionOrDefaultTimelineExpression(
				VariableTimelineExpression(content.story.customVariables.find {
					it.name == "TimeOfDay"
				}!! as CustomTimelineVariable<String?>),
				ConstantTimelineExpression(TimelineOptionalStringValue("Day"))
			),
			cases = arrayOf(
				SwitchCaseTimelineExpression.Case(
					inputToMatch = ConstantTimelineExpression(TimelineOptionalStringValue("Day")),
					outputWhenInputMatches = ConstantTimelineExpression(
						TimelineStringValue("Castle")
					)
				)
			),
			defaultOutput = ConstantTimelineExpression(TimelineOptionalStringValue(null))
		)
	)

	if (raw == "!GameData.plotVars.SUNSET ? \"WorldMap\" : \"crickets\"") {
		return timeOfDayMusic("WorldMap")
	}
	if (raw == "!GameData.plotVars.SUNSET ? \"Goznor\" : \"none\"") {
		return timeOfDayMusic("Goznor")
	}
	if (raw == "GoznorMusicExpression") {
		// TODO CHAP2 Change to "EvilStirs" during zombie outbreak
		return variableOrTimeOfDayMusic("GoznorMusic", "Goznor")
	}
	if (raw == "plotVars.PossessCut != 10 ? \"none\" : \"Rohoph\"") {
		// TODO CHAP1 Delete this clause (because it's unused)
		// , but make sure Rohoph music is played during the saucer conversation
		return variableOrDefaultMusic("RohophSaucerMusic", "none")
	}
	if (raw == "!GameData.plotVars.ELWYEN_DATE ? (!GameData.plotVars.SUNSET ? \"Castle\" : \"none\") : \"SirenSong\"") {
		// TODO CHAP3 Set to "SirenSong" during Elwyen date
		return castleGoznorMusic("CastleGoznorMusic")
	}
	if (raw == "!GameData.plotVars.ELWYEN_DATE ? (GameData.plotVars.EVIL_STEELE != 2 ? " +
			"(!(GameData.plotVars.BRIEFING == 4 && GameData.CHAPTER == 2 || GameData.plotVars.SUNSET) ? " +
			"(GameData.plotVars.BRIEFING != 3 ? \"Castle\" : \"RoyalGuard\") : \"none\") : " +
			"\"SomethingsAmiss\") : \"SirenSong\""
	) {
		// TODO CHAP2 Set to "RoyalGuard" during briefing
		// TODO CHAP3 Set to "SomethingAmiss" after coming back from Dark Temple
		// TODO CHAP3 Set to "SirenSong" during Elwyen date
		return castleGoznorMusic("CastleGoznorHallMusic")
	}
	if (raw == "!GameData.plotVars.ELWYEN_DATE ? (!(GameData.plotVars.CH3KING == 2 || " +
			"GameData.plotVars.CH3KING == 10) ? (GameData.plotVars.CH3KING != 1 ? " +
			"\"Castle\" : \"GdM\") : \"none\") : \"SirenSong\""
	) {
		// TODO CHAP3 Change to "GdM", or "SirenSong"
		return castleGoznorMusic("CastleGoznorThroneMusic")
	}
	if (raw == "GameData.plotVars.BRIEFING >= 4 ? \"Dungeon2\" : \"Muriance\"") {
		// TODO CHAP2 Change to "Muriance" before Muriance is slain
		return variableOrDefaultMusic("GemMinesMurianceRoomMusic", "Dungeon2")
	}
	if (raw == "GameData.plotVars.BEATEN_MORIC != null ? \"Catacombs\" : \"EvilStirs\"") {
		// TODO CHAP2 Set to "EvilStirs" before Moric is slain in catacombs
		return variableOrDefaultMusic("CatacombsMoricRoomMusic", "Catacombs")
	}
	if (raw == "GameData.plotVars.ARENA[GameData.CHAPTER] <= (GameData.CHAPTER != 3 ? 20 : 19) ? " +
			"\"ArenaBattle\" : \"VictoryFanfare2\""
	) {
		// TODO CHAP2 Set to "VictoryFanfare2" after winning 19 rounds, until Saviours show up
		// TODO CHAP3 Set to "VictoryFanfare2" after winning all 20 rounds
		return variableOrDefaultMusic("CambriaArenaAreaMusic", "ArenaBattle")
	}
	if (raw == "GameData.plotVars.ZOMBIES != \"CANONIA\" ? (GameData.plotVars.SUNSET != \"NIGHT\" ? " +
			"\"Canonia\" : \"crickets\") : \"EvilStirs\""
	) {
		// TODO CHAP2 Apply "EvilStirs" during zombie outbreak
		return variableOrTimeOfDayMusic("CanoniaMusic", "Canonia")
	}
	if (raw == "!(int(GameData.plotVars.ZACH) < 2 && GameData.CHAPTER == 2) ? \"Canonia\" : \"Zach\"") {
		// TODO CHAP2 Change to "Zach" before Zach is recruited
		return variableOrDefaultMusic("CanoniaInnMusic", "Canonia")
	}
	if (raw == "GameData.CHAPTER <= 2 ? (GameData.plotVars.ZOMBIES != \"CANONIA\" ? " +
			"(GameData.plotVars.SUNSET != \"NIGHT\" ? \"Canonia\" : \"crickets\") : \"EvilStirs\") : \"Gloria\""
	) {
		// TODO CHAP2 Apply "EvilStirs" during zombie outbreak
		// TODO CHAP3 Apply "Gloria" music track
		return variableOrTimeOfDayMusic("CanoniaCaveMusic", "Canonia")
	}
	if (raw == "GrottoBossRoomMusic") {
		// TODO CHAP2 Change to "EvilStirs" before the zombie shaman is slain
		return variableOrDefaultMusic("GrottoBossRoomMusic", "Dungeon1")
	}
	if (raw == "DoomCounter == null ? (GameData.plotVars.BEATEN_MORIC != 99 ? \"GdM\" : \"none\") : \"Flee\"") {
		// TODO CHAP2 Set to "none" and "Flee" after SocialMoric is slain
		return variableOrDefaultMusic("MoricShipBossRoomMusic", "GdM")
	}
	if (raw == "DoomCounter == null ? \"Battleship\" : \"Flee\"") {
		// TODO CHAP2 Set to "Flee" after defeating SocialMoric
		return variableOrDefaultMusic("MoricShipMusic", "Battleship")
	}
	if (raw == "SslenckOrReptoidVillage") {
		// TODO CHAP3 Set to "Sslenck" when Sslenck hasn't joined your party yet
		return variableOrDefaultMusic("XantusiaCityHallMusic", "ReptoidVillage")
	}
	if (raw == "GameData.plotVars.EVIL_STEELE != null ? \"DarkTemple\" : \"Steele\"") {
		// TODO CHAP3 Set to "Steele" when Steele hasn't been slain yet
		return variableOrDefaultMusic("DarkCrystalRoomMusic", "DarkTemple")
	}
	if (raw == "GameData.plotVars.ZACH == 4 ? \"Aeropolis\" : \"Zach\"") {
		// TODO CHAP3 Change to "Zach" before Zach is recruited
		return variableOrDefaultMusic("AeropolisInnMusic", "Aeropolis")
	}
	if (raw == "GameData.plotVars.WATER_CRYSTAL != null ? \"none\" : \"Crystals\"") {
		// TODO CHAP3 Change to "none" when water crystal is taken
		return variableOrDefaultMusic("WaterCrystalRoomMusic", "Crystals")
	}
	if (raw == "GameData.plotVars.FIRE_CRYSTAL != null ? \"none\" : \"Crystals\"") {
		// TODO CHAP3 Change to "none" when fire crystal is taken
		return variableOrDefaultMusic("FireCrystalRoomMusic", "Crystals")
	}
	if (
		raw == "int(GameData.plotVars.VEHRNCH3) < 4 ? \"Dungeon1\" : \"HymnOfYalort\"" ||
		raw == "GameData.plotVars.VEHRNCH3 < 4 ? \"Dungeon1\" : \"HymnOfYalort\""
	) {
		// TODO CHAP3 Set to "Dungeon1" before the monastery is cleared
		return variableOrDefaultMusic("LostMonasteryMusic", "HymnOfYalort")
	}
	if (raw == "GameData.plotVars.VEHRNCH3 < 4 ? \"EvilStirs\" : \"HymnOfYalort\"") {
		// TODO CHAP3 Set to "EvilStirs" before the monastery is cleared
		return variableOrDefaultMusic("LostMonasteryBossRoomMusic", "HymnOfYalort")
	}
	if (raw == "GameData.plotVars.FIRE_CRYSTAL != null ? \"none\" : (int(GameData.plotVars.FOUGHT_MURIANCE) >= 100 ? \"Crystals\" : \"Muriance\")") {
		// TODO CHAP3 Change to "Muriance" upon encountering Muriance, and to "none" after taking the crystal
		return variableOrDefaultMusic("EarthCrystalRoomMusic", "Crystals")
	}
	if (raw == "!GameData.plotVars.ELWYEN_DATE ? \"Aeropolis\" : \"SirenSong\"" || raw ==
			"GameData.plotVars.PLAY != 3 ? (!GameData.plotVars.ELWYEN_DATE ? \"Aeropolis\" : \"SirenSong\") : \"none\""
		) {
		// TODO CHAP3 Set to "SirenSong" during Elwyen date
		return variableOrDefaultMusic("AeropolisMusic", "Aeropolis")
	}

	throw RuntimeException("Unexpected music track $raw")
}

private fun parseAmbience(content: Content, raw: String?): TimelineExpression<ColorTransform> {
	if (raw == null || raw == "null") return ConstantTimelineExpression(
		TimelineColorTransformValue(ColorTransform.DEFAULT)
	)

	@Suppress("UNCHECKED_CAST")
	if (raw == "GenericExternalAmbience()") {
		return GlobalTimelineExpression(content.story.globalExpressions.find {
			it.name == "TimeOfDayAmbienceWithDefault"
		}!! as GlobalExpression<ColorTransform>)
	}

	if (raw.startsWith("{") && raw.endsWith("}")) {
		return parseRawAmbience(raw)
	}

	if (raw.startsWith("GenericExternalAmbience({") && raw.endsWith("})")) {
		val dayAmbience = parseRawAmbience(raw.substring("GenericExternalAmbience(".length, raw.length - 1))

		@Suppress("UNCHECKED_CAST")
		val darkAmbience = GlobalTimelineExpression(content.story.globalExpressions.find {
			it.name == "TimeOfDayAmbienceWithoutDefault"
		}!! as GlobalExpression<ColorTransform?>)
		return ExpressionOrDefaultTimelineExpression(darkAmbience, dayAmbience)
	}

	throw RuntimeException("can't deal with ambience $raw")
}

private fun parseRawAmbience(raw: String): TimelineExpression<ColorTransform> {
	val rawPairs = raw.substring(1, raw.length - 1).split(",")
	val pairs = rawPairs.map {
		val rawSplit = it.split(":")
		Pair(rawSplit[0], parseInt(rawSplit[1]))
	}
	val map = mutableMapOf(*pairs.toTypedArray())

	fun mul(key: String) = 0.01f * map[key]!!.toFloat()
	return ConstantTimelineExpression(TimelineColorTransformValue(ColorTransform(
		addColor = rgba(map["rb"]!!, map["gb"]!!, map["bb"]!!, map["ab"]!!),
		multiplyColor = rgba(mul("ra"), mul("ga"), mul("ba"), mul("aa")),
		subtractColor = 0,
	)))
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
