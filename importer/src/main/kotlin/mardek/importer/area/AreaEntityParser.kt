package mardek.importer.area

import mardek.assets.area.Direction
import mardek.assets.area.TransitionDestination
import mardek.assets.area.objects.*
import java.lang.Integer.parseInt
import kotlin.streams.toList

fun parseAreaEntities(rawEntities: String): List<Any> {
	return parseAreaEntities1(rawEntities).map(::parseAreaEntity2)
}

fun parseAreaEntities1(rawEntities: String): List<Map<String, String>> {

	val content = rawEntities.codePoints().toList()
	var depth = 0

	val STATE_KEY = 0
	val STATE_VALUE = 1
	var state = STATE_KEY

	val keyStorage = StringBuilder()
	val valueStorage = StringBuilder()
	val objectList = mutableListOf<Map<String, String>>()
	val currentObject = mutableMapOf<String, String>()

	for (character in content) {
		if (depth == 1) {
			if (character == ','.code || character == ']'.code) {
				objectList.add(HashMap(currentObject))
				currentObject.clear()
			}
		}

		if (depth == 2) {
			if (character == ','.code || character == '}'.code) {
				currentObject[keyStorage.toString()] = valueStorage.toString()
				keyStorage.clear()
				valueStorage.clear()
				state = STATE_KEY
				if (character == ','.code) continue
			}

			if (character == ':'.code) {
				parseAssert(state == STATE_KEY, "Unexpected : at depth $depth")
				state = STATE_VALUE
				continue
			}
		}

		if (character == '}'.code || character == ']'.code) depth -= 1

		if (depth >= 2) {
			if (state == STATE_KEY) keyStorage.appendCodePoint(character)
			else valueStorage.appendCodePoint(character)
		}

		if (character == '{'.code || character == '['.code) depth += 1
	}

	parseAssert(depth == 0, "Expected to end with depth 0, but got $depth")
	return objectList.toList()
}

fun parseAreaEntity2(rawEntity: Map<String, String>): Any {
	val model = parseFlashString(rawEntity["model"]!!, "model")!!
	val x = parseInt(rawEntity["x"])
	val y = parseInt(rawEntity["y"])

	if (model == "area_transition") return AreaTransition(
		x = x,
		y = y,
		arrow = if (rawEntity["ARROW"] != null) parseFlashString(rawEntity["ARROW"]!!, "ARROW")!! else null,
		destination = parseDestination(rawEntity["dest"]!!, rawEntity["dir"])
	)

	val name = parseFlashString(rawEntity["name"]!!, "name")!!
	if (name == "Dream Circle") return "Examine Dream Circle"

	if (model == "_trigger") return AreaTrigger(
		name = name,
		x = x,
		y = y,
		flashCode = rawEntity["ExecuteScript"]!!,
		oneTimeOnly = rawEntity["triggers"] != "-1",
		oncePerAreaLoad = rawEntity["recurring"] == "true",
		walkOn = rawEntity["WALKON"] == "true"
	)

	if (model == "talktrigger") return AreaTalkTrigger(
		name = name,
		x = x,
		y = y,
		npcName = parseFlashString(rawEntity["NPC"]!!, "talktrigger NPC")!!
	)

	val conversation = rawEntity["conv"]
	val conversationName = if (conversation != null && conversation.startsWith('"')) {
		parseFlashString(conversation, "conv")!!
	} else null
	val rawConversion = if (conversation != null && !conversation.startsWith('"')) conversation else null

	val silent = rawEntity["silent"] == "true"

	if (model.startsWith("o_")) {
		return AreaObject(
			spritesheetName = "obj_${model.substring(2)}",
			firstFrameIndex = null,
			numFrames = null,
			x = x,
			y = y,
			conversationName = conversationName,
			rawConversion = rawConversion,
			signType = null
		)
	}

	val rawDir = rawEntity["dir"]
	var direction: Direction? = null
	if (rawDir != null) direction = Direction.entries.find {
		it.abbreviation == parseFlashString(rawDir, "dir")!!
	}

	if (model.startsWith("BIGDOOR") || model.startsWith("DOOR")) {
		val (sheetName, spriteRow) = if (model.startsWith("B")) {
			Pair("BIGDOOR", parseInt(model.substring(7)))
		} else Pair("DOOR", parseInt(model.substring(4)))
		val destination = parseDestination(rawEntity["dest"]!!, rawEntity["dir"])
		val lockType = if (rawEntity.containsKey("lock")) parseFlashString(rawEntity["lock"]!!, "lock")!! else null
		return AreaDoor(
			spritesheetName = sheetName + "SHEET",
			spriteRow = spriteRow,
			x = x,
			y = y,
			destination = destination,
			lockType = lockType,
			keyName = if (rawEntity.containsKey("key")) parseFlashString(rawEntity["key"]!!, "key")!! else null
		)
	}

	val rawElement = rawEntity["elem"]
	val element = if (rawElement != null) parseFlashString(rawElement, "element")!! else null

	val spritesheetName = "spritesheet_${parseFlashString(rawEntity["model"]!!, "model")!!}"
	if (rawEntity["Static"] == "true" || rawEntity["Static"] == "1" || rawEntity.containsKey("FRAME")) {
		var firstFrame = 0
		var numFrames = 2

		if (direction != null) firstFrame = direction.ordinal

		val rawFrame = rawEntity["FRAME"]
		if (rawFrame != null) {
			firstFrame = parseInt(rawFrame)
			numFrames = 1
		}

		val rawSignType = rawEntity["sign"]
		val signType = if (rawSignType != null) parseFlashString(rawSignType, "sign type")!! else null

		return AreaObject(
			spritesheetName = spritesheetName,
			firstFrameIndex = firstFrame,
			numFrames = numFrames,
			x = x,
			y = y,
			conversationName = conversationName,
			rawConversion = rawConversion,
			signType = signType
		)
	}

	val rawPerson = rawEntity["EN"]
	val encyclopediaPerson = if (rawPerson != null) {
		val prefix = "[\"People\",\""
		val suffix = "\"]"
		parseAssert(rawPerson.startsWith(prefix), "Expected $rawPerson to start with $prefix")
		parseAssert(rawPerson.endsWith(suffix), "Expected $rawPerson to end with $suffix")
		rawPerson.substring(prefix.length, rawPerson.length - suffix.length)
	} else null

	return AreaCharacter(
		name = name,
		spritesheetName = spritesheetName,
		startX = x,
		startY = y,
		startDirection = direction,
		silent = silent,
		walkSpeed = parseInt(rawEntity["walkspeed"]),
		element = element,
		conversationName = conversationName,
		rawConversation = rawConversion,
		encyclopediaPerson = encyclopediaPerson
	)
}

private fun parseDestination(rawDestination: String, dir: String?): TransitionDestination {
	parseAssert(rawDestination.startsWith("["), "Expected dest $rawDestination to start with [")
	parseAssert(rawDestination.endsWith("]"), "Expected dest $rawDestination to end with ]")

	val splitDestination = rawDestination.substring(1, rawDestination.length - 1).split(",")
	parseAssert(splitDestination.size == 3, "Expected $rawDestination to have 2 ,s")
	return TransitionDestination(
		areaName = parseFlashString(splitDestination[0], "transition destination")!!,
		x = parseInt(splitDestination[1]),
		y = parseInt(splitDestination[2]),
		direction = if (dir != null) {
			Direction.entries.find { it.abbreviation == parseFlashString(dir, "dir")!! }!!
		} else null
	)
}
