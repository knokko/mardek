package mardek.importer.area

import mardek.assets.area.Direction
import mardek.assets.area.TransitionDestination
import mardek.assets.area.objects.*
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Integer.parseInt
import java.util.*
import kotlin.collections.ArrayList

private inline fun <reified T> extract(objectList: MutableList<Any>): ArrayList<T> {
	val result = ArrayList<T>(objectList.count { it is T })
	objectList.removeIf { candidate ->
		if (candidate is T) {
			result.add(candidate)
			true
		} else false
	}
	return result
}

fun parseAreaObjectsToList(rawEntities: String) = parseActionScriptObjectList(rawEntities).map(::parseAreaEntity)

fun parseAreaObjects(rawEntities: String, extraDecorations: List<AreaDecoration>): AreaObjects {
	val objectList = parseAreaObjectsToList(rawEntities).toMutableList()
	return AreaObjects(
		transitions = extract(objectList),
		walkTriggers = extract(objectList),
		talkTriggers = extract(objectList),
		shops = extract(objectList),
		decorations = ArrayList(extract<AreaDecoration>(objectList) + extraDecorations),
		portals = extract(objectList),
		objects = extract(objectList),
		characters = extract(objectList),
		doors = extract(objectList),
		switchOrbs = extract(objectList),
		switchGates = extract(objectList),
		switchPlatforms = extract(objectList)
	)
}

fun parseAreaEntity(rawEntity: Map<String, String>): Any {
	val model = parseFlashString(rawEntity["model"]!!, "model")!!
	val x = parseInt(rawEntity["x"])
	val y = parseInt(rawEntity["y"])

	val name = parseFlashString(rawEntity["name"]!!, "name")!!
	if (name == "Dream Circle") return "Examine Dream Circle"

	val conversation = rawEntity["conv"]
	val conversationName = if (conversation != null && conversation.startsWith('"')) {
		parseFlashString(conversation, "conv")!!
	} else null
	val rawConversation = if (conversation != null && !conversation.startsWith('"')) conversation else null

	if (model == "object" || model == "examine") {
		val rawType = rawEntity["type"]
		val rawColor = rawEntity["colour"]
		if (rawColor != null && rawType != null) {
			val color = SwitchColor.entries.find { it.name.lowercase(Locale.ROOT) == parseFlashString(rawColor, "colour") }
			val type = parseFlashString(rawType, "type")
			if (color != null) {
				if (type == "switch_orb") return AreaSwitchOrb(x = x, y = y, color = color)
				if (type == "switch_gate") return AreaSwitchGate(x = x, y = y, color = color)
				if (type == "switch_platform") return AreaSwitchPlatform(x = x, y = y, color = color)
			}
		}

		if (rawType == "\"examine\"" || model == "examine") return AreaDecoration(
			x = x, y = y, spritesheetName = null, spritesheetOffsetY = null, spriteHeight = null, light = null,
			timePerFrame = 1, rawConversation = rawConversation, conversationName = conversationName
		)
	}

	if (model == "shop") {
		val (shopName, waresConstantName) = parseShop(rawEntity["SHOP"]!!)
		return AreaShop(shopName = shopName, x = x, y = y, waresConstantName = waresConstantName)
	}

	if (model == "area_transition") return AreaTransition(
		x = x,
		y = y,
		arrow = if (rawEntity["ARROW"] != null) parseFlashString(rawEntity["ARROW"]!!, "ARROW")!! else null,
		destination = parseDestination(rawEntity["dest"]!!, rawEntity["dir"])
	)

	if (model == "_trigger") {
		val flashCode = rawEntity["ExecuteScript"]!!
		val warpPrefix = "_root.WarpTrans("
		if (flashCode.contains(warpPrefix)) {
			val startIndex = flashCode.indexOf(warpPrefix) + warpPrefix.length
			val endIndex = flashCode.indexOf(")", startIndex)
			val destination = parseDestination(flashCode.substring(startIndex, endIndex), null)
			val numSemicolons = flashCode.count { it == ';' }
			var isSimplePortalOrDreamCircle = numSemicolons == 1
			if (flashCode.contains("_root.ExitDreamrealm();") && numSemicolons == 2) {
				isSimplePortalOrDreamCircle = true
			}
			if (flashCode.contains("_root.EnterDreamrealm();") && numSemicolons == 3) {
				isSimplePortalOrDreamCircle = true
			}
			if (flashCode.contains("!HASPLOTITEM(\"Talisman of ONEIROS\")") && numSemicolons == 2) {
				isSimplePortalOrDreamCircle = true
			}

			if (isSimplePortalOrDreamCircle) return AreaPortal(x = x, y = y, destination = destination)
		}

		val rawWalkOn = rawEntity["WALKON"]

		return AreaTrigger(
			name = name,
			x = x,
			y = y,
			flashCode = flashCode,
			oneTimeOnly = rawEntity["triggers"] != "-1",
			oncePerAreaLoad = rawEntity["recurring"] == "true",
			walkOn = if (rawWalkOn != null) { if (rawWalkOn == "true") true else null } else false
		)
	}

	if (model == "talktrigger") return AreaTalkTrigger(
		name = name,
		x = x,
		y = y,
		npcName = parseFlashString(rawEntity["NPC"]!!, "talktrigger NPC")!!
	)


	val silent = rawEntity["silent"] == "true"

	if (model.startsWith("o_")) {
		val spritesheetName = "obj_${model.substring(2)}"
		return if (rawEntity["walkable"] == "true") AreaDecoration(
			x = x,
			y = y,
			spritesheetName = spritesheetName,
			spritesheetOffsetY = null,
			spriteHeight = null,
			light = null,
			timePerFrame = 1,
			conversationName = conversationName,
			rawConversation = rawConversation
		) else AreaObject(
			spritesheetName = spritesheetName,
			firstFrameIndex = null,
			numFrames = null,
			x = x,
			y = y,
			conversationName = conversationName,
			rawConversion = rawConversation,
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
		val lockType = if (rawEntity.containsKey("lock")) parseFlashString(rawEntity["lock"]!!, "lock") else null
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

		if (direction != null) firstFrame = 2 * direction.ordinal

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
			rawConversion = rawConversation,
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
		walkSpeed = parseInt(rawEntity["walkspeed"] ?: "-2"),
		element = element,
		conversationName = conversationName,
		rawConversation = rawConversation,
		encyclopediaPerson = encyclopediaPerson
	)
}

private fun parseDestination(rawDestination: String, dir: String?): TransitionDestination {
	parseAssert(rawDestination.startsWith("["), "Expected dest $rawDestination to start with [")
	parseAssert(rawDestination.endsWith("]"), "Expected dest $rawDestination to end with ]")

	val splitDestination = rawDestination.substring(1, rawDestination.length - 1).split(",")
	parseAssert(
		splitDestination.size == 3 || splitDestination.size == 4,
		"Expected $rawDestination to have 2 or 3 ','s"
	)
	return TransitionDestination(
		areaName = parseFlashString(splitDestination[0], "transition destination")!!,
		x = parseInt(splitDestination[1]),
		y = try { parseInt(splitDestination[2]) } catch (complicated: NumberFormatException) {
			println("weird split destination ${splitDestination[2]}"); -1 },
		direction = if (dir != null) {
			Direction.entries.find { it.abbreviation == parseFlashString(dir, "dir")!! }!!
		} else null,
		discoveredAreaName = if (splitDestination.size == 3) null else parseFlashString(
			splitDestination[3], "discovered area"
		)
	)
}

private fun parseShop(rawShop: String): Pair<String, String> {
	val separator = ",wares:DefaultShops."
	val prefix = "{name:"
	val index1 = rawShop.indexOf(separator)
	parseAssert(index1 >= 0, "Expected $rawShop to contain $separator")
	parseAssert(rawShop.startsWith(prefix), "Expected $rawShop to start with $prefix")
	parseAssert(rawShop.endsWith('}'), "Expected $rawShop to end with }")

	val name = parseFlashString(rawShop.substring(prefix.length, index1), "shop name")!!
	val wares = rawShop.substring(index1 + separator.length, rawShop.length - 1)
	return Pair(name, wares)
}
