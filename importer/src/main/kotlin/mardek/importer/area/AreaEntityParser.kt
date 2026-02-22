package mardek.importer.area

import mardek.content.Content
import mardek.content.action.ActionNode
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.FixedAction
import mardek.content.area.AreaTransitionDestination
import mardek.content.area.Direction
import mardek.content.area.TransitionDestination
import mardek.content.area.WorldMapTransitionDestination
import mardek.content.area.objects.*
import mardek.content.sprite.ObjectSprites
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.TimelineBooleanValue
import mardek.content.story.TimelineExpression
import mardek.importer.actions.HardcodedActions
import mardek.importer.actions.fixedActionChain
import mardek.importer.story.expressions.HardcodedExpressions
import mardek.importer.util.compressKimSprite3
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObjectList
import java.awt.Color
import java.lang.Integer.parseInt
import java.nio.charset.StandardCharsets
import java.util.UUID
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

internal class AreaEntityParseContext(
	val content: Content,
	val areaName: String,
	val actions: HardcodedActions,
	val expressions: HardcodedExpressions,
)

internal fun parseAreaObjectsToList(
	context: AreaEntityParseContext, rawEntities: String,
) = parseActionScriptObjectList(rawEntities).map {
	rawEntity -> parseAreaEntity(context, rawEntity)
}

internal fun parseAreaObjects(
	context: AreaEntityParseContext, rawEntities: String, extraDecorations: List<AreaDecoration>,
): AreaObjects {
	val objectList = parseAreaObjectsToList(context, rawEntities).toMutableList()
	return AreaObjects(
		transitions = extract(objectList),
		walkTriggers = extract(objectList),
		talkTriggers = extract(objectList),
		shops = extract(objectList),
		decorations = ArrayList(extract<AreaDecoration>(objectList) + extraDecorations),
		portals = extract(objectList),
		characters = extract(objectList),
		doors = extract(objectList),
		switchOrbs = extract(objectList),
		switchGates = extract(objectList),
		switchPlatforms = extract(objectList)
	)
}

internal fun importObjectSprites(
	flashName: String, frameIndex: Int = 0, numFrames: Int? = null,
	offsetY: Int = 0, height: Int? = null, skipTransparentFrames: Boolean = false,
): ObjectSprites {
	val imageName = flashName.replace("spritesheet_", "").replace("obj_", "")
	val imagePath = "sheets/objects/$imageName.png"
	val input = ParsedArea::class.java.getResourceAsStream(imagePath) ?:
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
	}.toMutableList()

	if (skipTransparentFrames) {
		while (images.size > 1) {
			val lastImage = images.last()
			var isEmpty = true
			for (y in 0 until lastImage.height) {
				for (x in 0 until lastImage.width) {
					if (Color(lastImage.getRGB(x, y), true).alpha > 0) isEmpty = false
				}
			}
			if (isEmpty) {
				images.removeLast()
			} else {
				break
			}
		}
	}

	return ObjectSprites(
			flashName = flashName,
			frameIndex = frameIndex,
			offsetY = offsetY,
			numFrames = numFrames,
			frames = images.map(::compressKimSprite3).toTypedArray()
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
	return SwitchColor(
		name, offSprite, onSprite, gateSprite, platformSprite,
		UUID.nameUUIDFromBytes("importSwitchColor$name".encodeToByteArray()),
	)
}

private fun attemptToExtractSimpleDialogue(
	sharedActions: ActionSequence?, rawDialogue: String?, baseID: UUID
): ActionNode? {
	if (rawDialogue == null || sharedActions != null) return null
	val conversationList = parseActionScriptNestedList(rawDialogue)
	if (conversationList is ArrayList<*>) {
		val actions: Array<FixedAction> = conversationList.map {
			if (it !is ArrayList<*>) return null
			ActionTalk(
				speaker = ActionTargetDefaultDialogueObject(),
				expression = parseFlashString(it[0].toString(), "dialogue expression") ?: return null,
				text = parseFlashString(it[1].toString(), "dialogue text") ?: return null,
			)
		}.toTypedArray()
		val ids = actions.indices.map {
			UUID(baseID.mostSignificantBits - it, baseID.leastSignificantBits + it)
		}.toTypedArray()
		if (conversationList.isEmpty()) return null
		return fixedActionChain(actions, ids)!!
	}
	return null
}

internal fun parseAreaEntity(context: AreaEntityParseContext, rawEntity: Map<String, String>): Any {
	val rawID = rawEntity["uuid"]
	val id = if (rawID != null) UUID.fromString(rawID) else {
		UUID.nameUUIDFromBytes((context.areaName + rawEntity.toString()).encodeToByteArray())
	}
	val model = parseFlashString(rawEntity["model"]!!, "model")!!
	val x = parseInt(rawEntity["x"])
	val y = parseInt(rawEntity["y"])

	val name = parseFlashString(rawEntity["name"]!!, "name")!!
	if (name == "Dream Circle") return "Examine Dream Circle"

	val conversation = rawEntity["conv"]
	val conversationName = if (conversation != null && conversation.startsWith('"')) {
		parseFlashString(conversation, "conv")!!
	} else null

	var actionSequence: ActionSequence? = null
	if (conversationName != null) {
		actionSequence = context.actions.getHardcodedAreaActions(context.areaName, conversationName) ?:
				context.actions.getHardcodedGlobalActionSequence(conversationName)
	}
	val rawConversation = if (conversation != null && !conversation.startsWith('"')) conversation else null

	val rawConditionName = rawEntity["timelineCondition"]
	val timelineCondition = if (rawConditionName != null) {
		val condition = context.expressions.getHardcodedAreaExpressions(
			context.areaName, rawConditionName
		) ?: context.expressions.getHardcodedGlobalExpressions(rawConditionName)!!

		@Suppress("UNCHECKED_CAST")
		condition as TimelineExpression<Boolean>
	} else null

	if (model == "object" || model == "examine") {
		val rawType = rawEntity["type"]
		val rawColor = rawEntity["colour"]
		if (rawColor != null && rawType != null) {
			val colorName = parseFlashString(rawColor, "colour")!!
			var color = context.content.areas.switchColors.find { it.name == colorName }
			if (color == null) {
				color = importSwitchColor(colorName)
				context.content.areas.switchColors.add(color)
			}
			val type = parseFlashString(rawType, "type")
			if (type == "switch_orb") return AreaSwitchOrb(x = x, y = y, color = color)
			if (type == "switch_gate") return AreaSwitchGate(x = x, y = y, color = color)
			if (type == "switch_platform") return AreaSwitchPlatform(x = x, y = y, color = color)
		}

		if (rawType == "\"examine\"" || model == "examine") {
			return AreaDecoration(
				x = x,
				y = y,
				sprites = null,
				canWalkThrough = true,
				light = null,
				timePerFrame = 1,
				ownActions = attemptToExtractSimpleDialogue(
					actionSequence, rawConversation, id,
				),
				sharedActionSequence = actionSequence,
				signType = null,
				displayName = name,
			)
		}
	}

	if (model == "sign") {
		val rawFrame = rawEntity["FRAME"] ?: throw IllegalArgumentException("Invalid sign $rawEntity")
		val firstFrame = parseInt(rawFrame)

		val rawSignType = rawEntity["sign"] ?: throw IllegalArgumentException("Invalid sign $rawEntity")
		val signType = parseFlashString(rawSignType, "sign type")!!

		val spriteID = "spritesheet_sign($firstFrame, 1)"
		var sprites = context.content.areas.objectSprites.find { it.flashName == spriteID }
		if (sprites == null) {
			sprites = importObjectSprites("spritesheet_sign", frameIndex = firstFrame, numFrames = 1)
			sprites.flashName = spriteID
			context.content.areas.objectSprites.add(sprites)
		}

		return AreaDecoration(
			x = x,
			y = y,
			sprites = sprites,
			canWalkThrough = false,
			light = null,
			timePerFrame = 1,
			ownActions = attemptToExtractSimpleDialogue(
				actionSequence, rawConversation, id,
			),
			sharedActionSequence = actionSequence,
			signType = signType,
			displayName = name,
		)
	}

	if (model == "shop") {
		val (shopName, waresConstantName) = parseShop(rawEntity["SHOP"]!!)
		return AreaShop(shopName = shopName, x = x, y = y, waresConstantName = waresConstantName)
	}

	if (model == "area_transition") return AreaTransition(
		x = x,
		y = y,
		arrow = if (rawEntity["ARROW"] != null) context.content.areas.arrowSprites.find {
			it.flashName == parseFlashString(rawEntity["ARROW"]!!, "ARROW")!!
		}!! else null,
		destination = parseDestination(
			rawEntity["dest"]!!, rawEntity["dir"], context.areaName
		)
	)

	if (model == "_trigger") {
		val flashCode = rawEntity["ExecuteScript"]
		val warpPrefix = "_root.WarpTrans("
		if (flashCode != null && flashCode.contains(warpPrefix)) {
			val startIndex = flashCode.indexOf(warpPrefix) + warpPrefix.length
			val endIndex = flashCode.indexOf(")", startIndex)
			val destination = parseDestination(
				flashCode.substring(startIndex, endIndex), null, context.areaName
			)
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
		val rawSequenceName = rawEntity["actionSequence"]

		val actionSequence = if (rawSequenceName != null) {
			context.actions.getHardcodedAreaActions(context.areaName, rawSequenceName) ?:
					context.actions.getHardcodedGlobalActionSequence(rawSequenceName)
		} else null



		return AreaTrigger(
			name = name,
			x = x,
			y = y,
			flashCode = flashCode,
			oneTimeOnly = rawEntity["triggers"] != "-1",
			oncePerAreaLoad = rawEntity["recurring"] == "true",
			walkOn = if (rawWalkOn != null) { if (rawWalkOn == "true") true else null } else false,
			actions = actionSequence,
			condition = timelineCondition,
			id = id,
		)
	}

	if (model == "talktrigger") {
		val talkX = if (rawEntity.containsKey("talkX")) parseInt(rawEntity["talkX"]) else -1
		val talkY = if (rawEntity.containsKey("talkY")) parseInt(rawEntity["talkY"]) else -1
		return AreaTalkTrigger(
			name = name,
			x = x,
			y = y,
			talkX = talkX,
			talkY = talkY,
			condition = timelineCondition,
		)
	}

	if (model.startsWith("o_")) {
		val spritesheetName = "obj_${model.substring(2)}"
		var sprites = context.content.areas.objectSprites.find { it.flashName == spritesheetName }
		if (sprites == null) {
			sprites = importObjectSprites(spritesheetName)
			context.content.areas.objectSprites.add(sprites)
		}
		return AreaDecoration(
			x = x,
			y = y,
			sprites = sprites,
			canWalkThrough = rawEntity["walkable"] == "true",
			light = null,
			timePerFrame = 200,
			ownActions = attemptToExtractSimpleDialogue(
				actionSequence, rawConversation, id,
			),
			sharedActionSequence = actionSequence,
			signType = null,
			displayName = name,
		)
	}

	val rawDir = rawEntity["dir"]
	var direction = Direction.Down
	if (rawDir == "RDir()") direction = Direction.Random
	else if (rawDir != null) direction = Direction.entries.find {
		it.abbreviation == parseFlashString(rawDir, "dir")!!
	} ?: Direction.Down

	if (model.startsWith("BIGDOOR") || model.startsWith("DOOR")) {
		val (sheetName, spriteRow, spriteHeight) = if (model.startsWith("B")) {
			Triple("BIGDOOR", parseInt(model.substring(7)), 32)
		} else Triple("DOOR", parseInt(model.substring(4)), 16)
		val destination = parseDestination(
			rawEntity["dest"]!!, rawEntity["dir"], context.areaName
		)

		val canOpen: TimelineExpression<Boolean>
		val cannotOpenActions: ActionSequence?
		val rawLock = rawEntity["lock"]

		val keyName = if (rawEntity.containsKey("key")) {
			parseFlashString(rawEntity["key"]!!, "key")!!
		} else null

		if (rawLock == null || rawLock == "null") {
			canOpen = ConstantTimelineExpression(TimelineBooleanValue(true))
			cannotOpenActions = null
		} else {
			if (rawLock.startsWith('"') && rawLock.endsWith('"')) {
				val lockType = rawLock.substring(1, rawLock.length - 1)
				if (keyName == null) {
					val condition = context.expressions.getHardcodedAreaExpressions(
						context.areaName, "lock_$lockType"
					) ?: context.expressions.getHardcodedGlobalExpressions("lock_$lockType")
					if (condition == null) {
						canOpen = ConstantTimelineExpression(TimelineBooleanValue(false))
					} else {
						@Suppress("UNCHECKED_CAST")
						canOpen = condition as TimelineExpression<Boolean>
					}
				} else {
					val key = context.content.items.plotItems.find { it.displayName == keyName } ?:
							throw RuntimeException("Can't find key $keyName")
					// TODO CHAP2 Use the key
					println("Should use key $key for lock $rawLock")
					canOpen = ConstantTimelineExpression(TimelineBooleanValue(false))
				}

				cannotOpenActions = context.actions.getHardcodedAreaActions(
					context.areaName, "lock_$lockType"
				) ?: context.actions.getHardcodedGlobalActionSequence("lock_$lockType")
			} else {
				println("Can't handle complex lock $rawLock")
				canOpen = ConstantTimelineExpression(TimelineBooleanValue(true))
				cannotOpenActions = null
			}
		}

		val spriteID = sheetName + spriteRow
		var sprites = context.content.areas.objectSprites.find { it.flashName == spriteID }
		if (sprites == null) {
			sprites = importObjectSprites(
					sheetName + "SHEET", offsetY = spriteHeight * spriteRow,
				height = spriteHeight, skipTransparentFrames = true,
			)
			sprites.flashName = spriteID
			context.content.areas.objectSprites.add(sprites)
		}
		return AreaDoor(
			id = UUID.nameUUIDFromBytes(
				"${context.areaName}$rawEntity".toByteArray(StandardCharsets.UTF_8)
			),
			sprites = sprites,
			x = x,
			y = y,
			destination = destination,
			canOpen = canOpen,
			cannotOpenActions = cannotOpenActions,
			displayName = name,
		)
	}

	val rawElement = rawEntity["elem"]
	val elementName = if (rawElement != null) parseFlashString(rawElement, "element")!! else null
	val element = if (elementName != null) context.content.stats.elements.find { it.rawName == elementName }!! else null

	val portrait = context.content.portraits.info.find { it.flashName.equals(model, ignoreCase = true) }
	var spritesheetName = "spritesheet_$model"
	if (rawEntity["Static"] == "true" || rawEntity["Static"] == "1" || rawEntity.containsKey("FRAME")) {
		var firstFrame = 2 * direction.ordinal
		var numFrames = 2

		val rawFrame = rawEntity["FRAME"]
		if (rawFrame != null) {
			firstFrame = parseInt(rawFrame)
			numFrames = 1
		}

		val spriteID = "$spritesheetName($firstFrame, $numFrames)"
		var sprites = context.content.areas.objectSprites.find { it.flashName == spriteID }
		if (sprites == null) {
			sprites = importObjectSprites(spritesheetName, frameIndex = firstFrame, numFrames = numFrames)
			sprites.flashName = spriteID
			context.content.areas.objectSprites.add(sprites)
		}

		return AreaCharacter(
			id = id,
			name = name,
			directionalSprites = null,
			fixedSprites = sprites,
			startX = x,
			startY = y,
			startDirection = Direction.Down,
			walkBehavior = WalkBehavior(0f, false),
			element = element,
			portrait = portrait,
			ownActions = attemptToExtractSimpleDialogue(
				actionSequence, rawConversation, id
			),
			sharedActionSequence = actionSequence,
			encyclopediaPerson = null,
			condition = timelineCondition,
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
	val sprites = context.content.areas.characterSprites.find { it.name == spritesheetName }!!
	val rawWalkSpeed = parseInt(rawEntity["walkspeed"] ?: "-2")
	val movesPerSecond = if (rawWalkSpeed > 0) 30f / rawWalkSpeed else 0f
	return AreaCharacter(
		id = id,
		name = name,
		directionalSprites = sprites,
		fixedSprites = null,
		startX = x,
		startY = y,
		startDirection = direction,
		walkBehavior = WalkBehavior(movesPerSecond, rawWalkSpeed == -1),
		element = element,
		portrait = portrait,
		ownActions = attemptToExtractSimpleDialogue(
			actionSequence, rawConversation, id
		),
		sharedActionSequence = actionSequence,
		encyclopediaPerson = encyclopediaPerson,
		condition = timelineCondition,
	)
}

private fun parseDestination(rawDestination: String, dir: String?, currentAreaName: String): TransitionDestination {
	parseAssert(rawDestination.startsWith("["), "Expected dest $rawDestination to start with [")
	parseAssert(rawDestination.endsWith("]"), "Expected dest $rawDestination to end with ]")

	val splitDestination = rawDestination.substring(1, rawDestination.length - 1).split(",")
	parseAssert(
		splitDestination.size == 3 || splitDestination.size == 4,
		"Expected $rawDestination to have 2 or 3 ','s"
	)
	var areaName = parseFlashString(splitDestination[0], "transition destination")!!
	if (areaName == "nowhere") areaName = "goznor"
	val destination = if (areaName == "WORLDMAP") WorldMapTransitionDestination(currentAreaName)
	else AreaTransitionDestination(
		areaName = areaName,
		x = parseInt(splitDestination[1]),
		y = try { parseInt(splitDestination[2]) } catch (_: NumberFormatException) {
			println("weird split destination ${splitDestination[2]}"); -1 },
		direction = if (dir != null) {
			Direction.entries.find { it.abbreviation == parseFlashString(dir, "dir")!! }!!
		} else null,
	)

	return destination
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
