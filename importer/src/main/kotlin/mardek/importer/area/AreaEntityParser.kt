package mardek.importer.area

import mardek.content.area.AreaContent
import mardek.content.area.Direction
import mardek.content.area.TransitionDestination
import mardek.content.area.objects.*
import mardek.content.sprite.ObjectSprites
import mardek.importer.util.compressKimSprite1
import mardek.importer.util.parseActionScriptObjectList
import java.lang.Integer.parseInt
import javax.imageio.ImageIO
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

fun parseAreaObjectsToList(
	assets: AreaContent, rawEntities: String,
	transitions: MutableList<Pair<TransitionDestination, String>>
) = parseActionScriptObjectList(rawEntities).map {
	rawEntity -> parseAreaEntity(assets, rawEntity, transitions)
}

internal fun parseAreaObjects(
	assets: AreaContent, rawEntities: String, extraDecorations: List<AreaDecoration>,
	transitions: MutableList<Pair<TransitionDestination, String>>
): AreaObjects {
	val objectList = parseAreaObjectsToList(assets, rawEntities, transitions).toMutableList()
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

internal fun importObjectSprites(
	flashName: String, frameIndex: Int = 0, numFrames: Int? = null,
	offsetY: Int = 0, height: Int? = null
): ObjectSprites {
	val imageName = flashName.replace("spritesheet_", "").replace("obj_", "")
	val imagePath = "sheets/objects/$imageName.png"
	val input = AreaSprites::class.java.getResourceAsStream(imagePath) ?:
			throw IllegalArgumentException("Can't get resource $imagePath")
	val sheetImage = ImageIO.read(input)
	input.close()

	var spriteHeight = height ?: sheetImage.height
	if (flashName == "chests") spriteHeight = 16

	// Disgusting formula, but I can't find anything logical
	val spriteWidth = if (flashName.startsWith("spritesheet_")) {
		16 * (sheetImage.height / 16)
	} else 16

	val images = ((0 until (numFrames ?: (sheetImage.width / spriteWidth)))).map {
		sheetImage.getSubimage(spriteWidth * (frameIndex + it), offsetY, spriteWidth, spriteHeight)
	}

	return ObjectSprites(
			flashName = flashName,
			frameIndex = frameIndex,
			offsetY = offsetY,
			numFrames = numFrames,
			frames = images.map(::compressKimSprite1).toTypedArray()
	)
}

private fun importSwitchColor(name: String): SwitchColor {
	val frameIndex = when (name) {
		"ruby" -> 0
		"amethyst" -> 1
		"moonstone" -> 2
		"emerald" -> 3
		"topaz" -> 4
		"turquoise" -> 5
		"sapphire" -> 6
		else -> throw UnsupportedOperationException("Unknown gem color $name")
	}
	val offSprite = importObjectSprites("switch_orb", frameIndex = 0, numFrames = 1).frames[0]
	val onSprite = importObjectSprites("switch_orb", frameIndex = frameIndex, numFrames = 1).frames[0]
	val gateSprite = importObjectSprites("switch_gate", frameIndex = frameIndex, numFrames = 1).frames[0]
	val platformSprite = importObjectSprites("switch_platform", frameIndex = frameIndex, numFrames = 1).frames[0]
	return SwitchColor(name, offSprite, onSprite, gateSprite, platformSprite)
}

fun parseAreaEntity(
	assets: AreaContent, rawEntity: Map<String, String>,
	transitions: MutableList<Pair<TransitionDestination, String>>
): Any {
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
			val colorName = parseFlashString(rawColor, "colour")!!
			var color = assets.switchColors.find { it.name == colorName }
			if (color == null) {
				color = importSwitchColor(colorName)
				assets.switchColors.add(color)
			}
			val type = parseFlashString(rawType, "type")
			if (type == "switch_orb") return AreaSwitchOrb(x = x, y = y, color = color)
			if (type == "switch_gate") return AreaSwitchGate(x = x, y = y, color = color)
			if (type == "switch_platform") return AreaSwitchPlatform(x = x, y = y, color = color)
		}

		if (rawType == "\"examine\"" || model == "examine") {
			return AreaDecoration(
					x = x, y = y, sprites = null, light = null, timePerFrame = 1,
					rawConversation = rawConversation, conversationName = conversationName
			)
		}
	}

	if (model == "shop") {
		val (shopName, waresConstantName) = parseShop(rawEntity["SHOP"]!!)
		return AreaShop(shopName = shopName, x = x, y = y, waresConstantName = waresConstantName)
	}

	if (model == "area_transition") return AreaTransition(
		x = x,
		y = y,
		arrow = if (rawEntity["ARROW"] != null) assets.arrowSprites.find {
			it.flashName == parseFlashString(rawEntity["ARROW"]!!, "ARROW")!!
		}!! else null,
		destination = parseDestination(rawEntity["dest"]!!, rawEntity["dir"], transitions)
	)

	if (model == "_trigger") {
		val flashCode = rawEntity["ExecuteScript"]!!
		val warpPrefix = "_root.WarpTrans("
		if (flashCode.contains(warpPrefix)) {
			val startIndex = flashCode.indexOf(warpPrefix) + warpPrefix.length
			val endIndex = flashCode.indexOf(")", startIndex)
			val destination = parseDestination(flashCode.substring(startIndex, endIndex), null, transitions)
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
		var sprites = assets.objectSprites.find { it.flashName == spritesheetName }
		if (sprites == null) {
			sprites = importObjectSprites(spritesheetName)
			assets.objectSprites.add(sprites)
		}
		return if (rawEntity["walkable"] == "true") AreaDecoration(
			x = x,
			y = y,
			sprites = sprites,
			light = null,
			timePerFrame = 1,
			conversationName = conversationName,
			rawConversation = rawConversation
		) else AreaObject(
			sprites = sprites,
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
		val (sheetName, spriteRow, spriteHeight) = if (model.startsWith("B")) {
			Triple("BIGDOOR", parseInt(model.substring(7)), 32)
		} else Triple("DOOR", parseInt(model.substring(4)), 16)
		val destination = parseDestination(rawEntity["dest"]!!, rawEntity["dir"], transitions)
		val lockType = if (rawEntity.containsKey("lock")) parseFlashString(rawEntity["lock"]!!, "lock") else null

		val spriteID = sheetName + spriteRow
		var sprites = assets.objectSprites.find { it.flashName == spriteID }
		if (sprites == null) {
			sprites = importObjectSprites(
					sheetName + "SHEET", offsetY = spriteHeight * spriteRow, height = spriteHeight
			)
			sprites.flashName = spriteID
			assets.objectSprites.add(sprites)
		}
		return AreaDoor(
			sprites = sprites,
			x = x,
			y = y,
			destination = destination,
			lockType = lockType,
			keyName = if (rawEntity.containsKey("key")) parseFlashString(rawEntity["key"]!!, "key")!! else null
		)
	}

	val rawElement = rawEntity["elem"]
	val element = if (rawElement != null) parseFlashString(rawElement, "element")!! else null

	var spritesheetName = "spritesheet_${parseFlashString(rawEntity["model"]!!, "model")!!}"
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

		val spriteID = "$spritesheetName($firstFrame, $numFrames)"
		var sprites = assets.objectSprites.find { it.flashName == spriteID }
		if (sprites == null) {
			sprites = importObjectSprites(spritesheetName, frameIndex = firstFrame, numFrames = numFrames)
			sprites.flashName = spriteID
			assets.objectSprites.add(sprites)
		}

		return AreaObject(
			sprites = sprites,
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

	spritesheetName = spritesheetName.replace("spritesheet_", "")
	val sprites = assets.characterSprites.find { it.name == spritesheetName }!!
	return AreaCharacter(
		name = name,
		sprites = sprites,
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

private fun parseDestination(
		rawDestination: String, dir: String?, transitions: MutableList<Pair<TransitionDestination, String>>
): TransitionDestination {
	parseAssert(rawDestination.startsWith("["), "Expected dest $rawDestination to start with [")
	parseAssert(rawDestination.endsWith("]"), "Expected dest $rawDestination to end with ]")

	val splitDestination = rawDestination.substring(1, rawDestination.length - 1).split(",")
	parseAssert(
		splitDestination.size == 3 || splitDestination.size == 4,
		"Expected $rawDestination to have 2 or 3 ','s"
	)
	val areaName = parseFlashString(splitDestination[0], "transition destination")
	val transition = TransitionDestination(
		area = null,
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

	if (areaName != null) transitions.add(Pair(transition, areaName))
	return transition
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
