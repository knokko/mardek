package mardek.importer.area

import com.github.knokko.bitser.Bitser
import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDialogueObject
import mardek.content.action.FixedActionNode
import mardek.content.area.Area
import mardek.content.area.Direction
import mardek.content.area.TransitionDestination
import mardek.content.area.objects.*
import mardek.importer.actions.HardcodedActions
import mardek.importer.importVanillaContent
import mardek.importer.story.expressions.HardcodedExpressions
import mardek.importer.util.parseActionScriptObjectList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNull
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestAreaEntityParser {

	private val content = importVanillaContent(Bitser(true), skipMonsters = true)

	@Test
	fun testSingleEntitySingleKey() {
		val rawEntities = "[{name:\"Guard\"}]"
		val parsed1 = parseActionScriptObjectList(rawEntities)
		assertEquals(1, parsed1.size)
		assertEquals(1, parsed1[0].size)
		assertEquals("\"Guard\"", parsed1[0]["name"])
	}

	@Test
	fun testSingleEntityTwoKeys() {
		val rawEntities = "[{name:\"Guard\",x:22}]"
		val parsed1 = parseActionScriptObjectList(rawEntities)
		println(parsed1)
		assertEquals(1, parsed1.size)
		assertEquals(2, parsed1[0].size)
		assertEquals("\"Guard\"", parsed1[0]["name"])
		assertEquals("22", parsed1[0]["x"])
	}

	@Test
	fun testTwoEntitiesWithSingleKey() {
		val rawEntities = "[{x:1},{y:2}]"
		val parsed1 = parseActionScriptObjectList(rawEntities)
		assertEquals(2, parsed1.size)
		assertEquals(1, parsed1[0].size)
		assertEquals("1", parsed1[0]["x"])
		assertEquals(1, parsed1[1].size)
		assertEquals("2", parsed1[1]["y"])
	}

	@Test
	fun testTwoEntitiesWithTwoKeys() {
		val rawEntities = "[{x:1,y:2},{x:3,y:4}]"
		val parsed1 = parseActionScriptObjectList(rawEntities)
		assertEquals(2, parsed1.size)
		assertEquals(2, parsed1[0].size)
		assertEquals("1", parsed1[0]["x"])
		assertEquals("2", parsed1[0]["y"])

		assertEquals(2, parsed1[1].size)
		assertEquals("3", parsed1[1]["x"])
		assertEquals("4", parsed1[1]["y"])
	}

	@Test
	fun testSingleComplexEntity() {
		val rawEntities = "[{name:\"Guard\",model:\"arabguard\",x:22,y:22,walkspeed:-1,dir:\"s\",elem:\"AIR\"," +
				"conv:[[\"sour\",\"This is the Air Temple. You may enter to pay your respects to the Air Crystal " +
				"or to receive blessings from the Priestess, but the deeper sections of the temple are off-limits " +
				"to people of no significance such as yourself.\"]]}]"
		val parsed1 = parseActionScriptObjectList(rawEntities)
		assertEquals(1, parsed1.size)
		assertEquals(8, parsed1[0].size)
		assertEquals("\"AIR\"", parsed1[0]["elem"])

		val expectedConversation = "[[\"sour\",\"This is the Air Temple. You may enter to pay your respects to the Air Crystal " +
				"or to receive blessings from the Priestess, but the deeper sections of the temple are off-limits " +
				"to people of no significance such as yourself.\"]]"
		assertEquals(expectedConversation, parsed1[0]["conv"])
	}

	private fun parseAreaEntityRaw(
		rawString: String, areaName: String = "",
		hardcodedActions: HardcodedActions = HardcodedActions()
	): Any {
		val context = AreaEntityParseContext(
			content, areaName, hardcodedActions, HardcodedExpressions(), mutableListOf()
		)
		val transitions = ArrayList<Pair<TransitionDestination, String>>()
		val parsedEntities = parseAreaObjectsToList(context, "[$rawString]")
		assertEquals(1, parsedEntities.size)

		for ((transition, destination) in transitions) {
			val area = Area()
			area.properties.rawName = destination
			transition.area = area
			content.areas.areas.add(area)
		}
		return parsedEntities[0]
	}

	private fun findArea(areaName: String) = content.areas.areas.find { it.properties.rawName == areaName }!!

	@Test
	fun testParseTransitionWithArrowWithoutDirection() {
		val actual = findArea("aeropolis_E_iShop").objects.transitions[0]
		val expected = AreaTransition(
			x = 3, y = 7, arrow = content.areas.arrowSprites.find { it.flashName == "S" }!!,
			destination = TransitionDestination(
				area = findArea("aeropolis_E"), worldMap = null, x = 22, y = 24, direction = null
			)
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseTransitionWithoutArrowWithDirection() {
		val actual = findArea("aeropolis_E").objects.transitions.find { it.x == 22 }!!
		val expected = AreaTransition(x = 22, y = 23, arrow = null, destination = TransitionDestination(
			area = findArea("aeropolis_E_iShop"), worldMap = null, x = 3, y = 6, direction = Direction.Up
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWalkTriggerSimple() {
		val actual = findArea("canonia_dreamcave_d").objects.walkTriggers.find { it.x == 8 }!!
		val expected = AreaTrigger(
			name = "INTERJECTION",
			x = 8,
			y = 5,
			flashCode = "function(){   _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");}",
			oneTimeOnly = true,
			oncePerAreaLoad = false,
			walkOn = false,
			actions = null,
			condition = null,
			id = actual.id,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWalkTriggerDreamCaveCircle() {
		val executeScript = "function()" +
				"{" +
				"   if(!_root.HasAlly(\"Gloria\") && !HASPLOTITEM(\"Talisman of ONEIROS\"))" +
				"   {" +
				"      return undefined;" +
				"   }" +
				"   if(!HASPLOTITEM(\"Talisman of ONEIROS\"))" +
				"   {" +
				"      MAKEPARTY([\"Mardek\",\"Gloria\",\"Elwyen\",\"Solaar\"],true);" +
				"   }" +
				"   _root.WarpTrans([\"canonia_dreamcave_d\",8,5]);" +
				"}"

		val actual = findArea("canonia_dreamcave").objects.walkTriggers.find { it.y == 5 }!!
		val expected = AreaTrigger(
			name = "TRANSPORT_TRIGGER",
			x = 8,
			y = 5,
			flashCode = executeScript,
			oneTimeOnly = false,
			oncePerAreaLoad = false,
			walkOn = true,
			actions = null,
			condition = null,
			id = actual.id,
		)
		assertEquals(expected, actual)
	}

	// TODO CHAP3 Revive this unit test?
//	@Test
//	fun testParseWalkTriggerInventor() {
//		val flashCode = "function(){\tDO_ACTIONS([[\"UNFREEZE\"],[\"TALK\",\"c_inventor\"]],\"PC\",true);}"
//
//		val actual = parseAreaEntityRaw(
//			"{uuid:$testID,name:\"TALK_TRIGGER\",model:\"_trigger\",x:3,y:5,triggers:1,recurring:true,ExecuteScript:$flashCode}"
//		)
//		//val actual = findArea("gz_house02").objects.walkTriggers.find { it.x == 3 }!!
//		val expected = AreaTrigger(
//			name = "TALK_TRIGGER",
//			x = 3,
//			y = 5,
//			oneTimeOnly = true,
//			oncePerAreaLoad = true,
//			flashCode = "function(){\tDO_ACTIONS([[\"UNFREEZE\"],[\"TALK\",\"c_inventor\"]],\"PC\",true);}",
//			walkOn = false,
//			actions = null,
//			condition = null,
//			id = actual.id,
//		)
//		assertEquals(expected, actual)
//	}

	@Test
	fun testParseTalkTrigger() {
		val actual = findArea("aeropolis_S_bazaar").objects.talkTriggers.find { it.x == 5 }!!
		val expected = AreaTalkTrigger(
			name = "TALKTRIGGER",
			x = 5,
			y = 2,
			npcName = "Shopkeeper"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSaveCrystal() {
		val actual = findArea("aeropolis_N_TAIR").objects.decorations.find { it.x == 4 }!!
		val expected = AreaDecoration(
			x = 4,
			y = 7,
			sprites = content.areas.objectSprites.find { it.flashName == "obj_Crystal" }!!,
			canWalkThrough = false,
			light = null,
			timePerFrame = 200,
			ownActions = null,
			conversationName = "c_healingCrystal",
			sharedActionSequence = content.actions.global.find { it.name == "c_healingCrystal" }!!,
			signType = null,
		)
		assertEquals(expected, actual)
	}

	// TODO CHAP2 Maybe revive this test
//	@Test
//	fun testParseSlainDracelon() {
//		val id = UUID.randomUUID()
//		val expected = AreaCharacter(
//			name = "Dracelon",
//			directionalSprites = null,
//			fixedSprites = objectSprite("spritesheet_ch2bosses(4, 1)"),
//			startX = 4,
//			startY = 21,
//			startDirection = Direction.Down,
//			walkSpeed = -2,
//			element = content.stats.elements.find { it.rawName == "EARTH" }!!,
//			portrait = null,
//			conversationName = "c_A_Rohoph",
//			rawConversation = null,
//			actionSequence = null,
//			encyclopediaPerson = null,
//			id = id,
//		)
//		val actual = parseAreaEntityRaw(
//				"{name:\"Dracelon\",model:\"ch2bosses\",x:4,y:21,walkspeed:-1,FRAME:4,silent:true," +
//						"Static:true,elem:\"EARTH\",conv:\"c_A_Rohoph\",uuid:$id}"
//		)
//		assertEquals(expected, actual)
//	}

	// TODO CHAP2 Maybe revive this unit test
//	@Test
//	fun testParseZombieDragon() {
//		val id = UUID.randomUUID()
//		val flashCode = "function()\n" +
//				"   {\n" +
//				"      _root.playSFX(\"dragon_roar\");\n" +
//				"      return 1;\n" +
//				"   },[\"angr\",\"Rrrrrrrrrrrrrrrrrrrrr!!!!\"],Do = function()\n" +
//				"   {\n" +
//				"      GameData.plotVars.ZDRAGON = 1;\n" +
//				"      BATTLE([[\"ZombieDragon\",null,null,null],[\"Zombie Dragon\",null,null,null],[18,null,null,null],\"DRAGON\"],\"BossBattle\",true);\n" +
//				"      return true;\n" +
//				"   }"
//		val expected = AreaCharacter(
//			name = "Zombie Dragon",
//			directionalSprites = null,
//			fixedSprites = objectSprite("spritesheet_dragon(2, 2)"),
//			startX = 11,
//			startY = 6,
//			startDirection = Direction.Down,
//			walkSpeed = -2,
//			element = content.stats.elements.find { it.rawName == "DARK" }!!,
//			portrait = null,
//			conversationName = null,
//			rawConversation = "[Do = $flashCode]",
//			actionSequence = null,
//			encyclopediaPerson = null,
//			id = id,
//		)
//		val actual = parseAreaEntityRaw(
//				"{name:\"Zombie Dragon\",model:\"dragon\",x:11,y:6,walkspeed:-1,dir:\"n\",Static:1," +
//						"elem:\"DARK\",conv:[Do = $flashCode],uuid:$id}"
//		)
//		assertEquals(expected, actual)
//	}

	@Test
	fun testParseTheDragon() {
		val id = UUID.fromString("6d8a7f59-5b45-4054-8266-49eae259fdbb")
		val expected = AreaCharacter(
			name = "The Dragon",
			directionalSprites = null,
			fixedSprites = content.areas.objectSprites.find { it.flashName ==  "spritesheet_dragon(0, 2)" },
			startX = 6,
			startY = 7,
			startDirection = Direction.Down,
			walkSpeed = -2,
			element = content.stats.elements.find { it.rawName == "DARK" }!!,
			portrait = null,
			conversationName = null,
			rawConversation = "[]",
			actionSequence = null,
			encyclopediaPerson = null,
			id = id,
		)
		val actual = findArea("DL_area4").objects.characters.find { it.startX == 6 && it.startY == 7 }!!
		assertEquals(expected, actual)
	}

	@Test
	fun testParseMoric() {
		val actual = findArea("crypt4").objects.characters.find { it.startY == 11 }!!
		val expected = AreaCharacter(
			name = "Moric",
			directionalSprites = null,
			fixedSprites = content.areas.objectSprites.find { it.flashName == "spritesheet_moric(1, 1)" },
			startX = 7,
			startY = 11,
			startDirection = Direction.Down,
			walkSpeed = -2,
			element = content.stats.elements.find { it.rawName == "EARTH" }!!,
			portrait = null,
			conversationName = "c_GdM_Moric",
			rawConversation = null,
			actionSequence = null,
			encyclopediaPerson = null,
			id = actual.id,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParsePriestessGail() {
		val actual = findArea("aeropolis_N_TAIR").objects.characters.find { it.startY == 3 }!!
		val expected = AreaCharacter(
			name = "Priestess Gail",
			directionalSprites = content.areas.characterSprites.find { it.name == "priestess" }!!,
			fixedSprites = null,
			startX = 7,
			startY = 3,
			startDirection = Direction.Down,
			walkSpeed = -1,
			element = content.stats.elements.find { it.rawName == "AIR" }!!,
			portrait = null, // Portraits are tested in IntegrationTests.testAreaCharacterPortraitImporting
			conversationName = "c_priestess",
			rawConversation = null,
			actionSequence = null,
			encyclopediaPerson = "Priestess Gail",
			id = actual.id,
		)
		assertEquals(expected, actual)
	}

	// TODO CHAP3 Maybe revive this test
//	@Test
//	fun testParseSign() {
//		val actual = findArea("aeropolis_W").objects.decorations.find { it.x == 26 && it.y == 26 }!!
//		val expected = AreaDecoration(
//			x = 26,
//			y = 26,
//			sprites = objectSprite("spritesheet_sign(10, 1)"),
//			canWalkThrough = false,
//			light = null,
//			timePerFrame = 1,
//			conversationName = null,
//			rawConversation = "[[\"\",\"CLOEST FORE THE NYHTE\"]]",
//			actionSequence = null,
//			signType = "words",
//		)
//		assertEquals(expected, actual)
//	}

	@Test
	fun testParseBigDoor() {
		val actual = findArea("crypt1").objects.doors.find { it.y == 13 }!!
		val expected = AreaDoor(
			id = actual.id,
			sprites = content.areas.objectSprites.find { it.flashName == "BIGDOOR3" }!!,
			x = 19,
			y = 13,
			destination = TransitionDestination(
				area = findArea("crypt2"), worldMap = null, x = 19, y = 13, direction = Direction.Down
			),
			lockType = null,
			keyName = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseLockedDoor() {
		val actual = findArea("aeropolis_E_warehouse").objects.doors.find { it.y == 8 }!!
		val expected = AreaDoor(
			id = actual.id,
			sprites = content.areas.objectSprites.find { it.flashName == "DOOR5" }!!,
			x = 4,
			y = 8,
			destination = TransitionDestination(
					findArea("aeropolis_E"), null, 13, 20, Direction.Down
			),
			lockType = "key",
			keyName = "Bandit Key"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseStatue() {
		val actual = findArea("lakequr").objects.characters.find { it.startX == 25 }!!
		val expected = AreaCharacter(
			name = "Statue",
			directionalSprites = null,
			fixedSprites = content.areas.objectSprites.find { it.flashName == "spritesheet_statue(6, 1)" }!!,
			startX = 25,
			startY = 3,
			startDirection = Direction.Down,
			walkSpeed = -2,
			element = null,
			portrait = null,
			conversationName = null,
			rawConversation = "[statueFlavour]",
			actionSequence = null,
			encyclopediaPerson = null,
			id = actual.id,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleExamine() {
		val actual = parseAreaEntityRaw(
			"{name:\"Dream Circle\",model:\"object\",type:\"examine\",x:23,y:61," +
					"walkable:true,conv:[[\"\",\"It\\'s a Dream Circle.\"]]}"
		)
		assertEquals("Examine Dream Circle", actual)
	}

	@Test
	fun testParseDreamCircleTriggerExit() {
		val actual = findArea("canonia_woods_d").objects.portals.find { it.y == 61 }!!
		val expected = AreaPortal(x = 23, y = 61, destination = TransitionDestination(
			findArea("canonia_woods"), null, 23, 61, direction = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleEnterCanoniaWoods() {
		val expected = AreaPortal(x = 23, y = 61, destination = TransitionDestination(
			findArea("canonia_woods_d"), null, 23, 61, direction = null
		))
		val actual = findArea("canonia_woods").objects.portals.find { it.y == 61 }!!
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleEnterTaintedGrotto() {
		val expected = AreaPortal(x = 33, y = 24, destination = TransitionDestination(
				findArea("pcave3_d"), null, 33, 24, direction = null
		))
		val actual = findArea("pcave3").objects.portals.find { it.x == 33 }!!
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleSpecialEnter() {
		val flashCode = "function()" +
		"{" +
				"   if(!_root.HasAlly(\"Gloria\") && !HASPLOTITEM(\"Talisman of ONEIROS\"))" +
				"   {" +
				"      return undefined;" +
				"   }" +
				"   if(!HASPLOTITEM(\"Talisman of ONEIROS\"))" +
				"   {" +
				"      MAKEPARTY([\"Mardek\",\"Gloria\",\"Elwyen\",\"Solaar\"],true);" +
				"   }" +
				"   _root.WarpTrans([\"canonia_dreamcave_d\",8,5]);" +
				"}"
		val actual = findArea("canonia_dreamcave").objects.walkTriggers.find { it.x == 8 }!!
		val expected = AreaTrigger(
			name = "TRANSPORT_TRIGGER",
			x = 8,
			y = 5,
			flashCode = flashCode,
			oneTimeOnly = false,
			oncePerAreaLoad = false,
			walkOn = true,
			actions = null,
			condition = null,
			id = actual.id,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleSpecialExit() {
		val flashCode = "function()" +
				"{" +
				"   if(!GameData.plotVars.Mardek_itj_dreamcave1)" +
				"   {" +
				"      _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");" +
				"   }" +
				"   else" +
				"   {" +
				"      _root.WarpTrans([\"canonia_dreamcave\",8,5]);" +
				"   }" +
				"}"
		val actual = findArea("canonia_dreamcave_d").objects.walkTriggers.find { it.name == "TRANSPORT_TRIGGER" }!!
		val expected = AreaTrigger(
			name = "TRANSPORT_TRIGGER",
			x = 8,
			y = 5,
			flashCode = flashCode,
			oneTimeOnly = false,
			oncePerAreaLoad = false,
			walkOn = null,
			actions = null,
			condition = null,
			id = actual.id,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseInvalidWarportPortal() {
		val actual = findArea("warport1T2").objects.decorations.find { it.x == 36 }!!
		val expected = AreaDecoration(
			sprites = content.areas.objectSprites.find { it.flashName == "obj_portal" },
			x = 36,
			y = 4,
			canWalkThrough = false,
			light = null,
			timePerFrame = 200,
			conversationName = null,
			ownActions = actual.ownActions!!,
			sharedActionSequence = null,
			signType = null,
		)
		val rootNode = actual.ownActions!!
		assertNull((rootNode as FixedActionNode).next)
		val action = rootNode.action as ActionTalk
		assertEquals(ActionTargetDialogueObject("Portal"), action.speaker)
		assertEquals("", action.expression)
		assertEquals("We hope you enjoyed your trip. Please leave the arrivals area.", action.text)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWarportExamine() {
		val actual = findArea("warport2T3").objects.decorations.find { it.x == 6 && it.y == 6 }!!
		val expected = AreaDecoration(
			x = 6,
			y = 6,
			sprites = content.areas.objectSprites.find { it.flashName == "obj_portal" }!!,
			canWalkThrough = true,
			light = null,
			timePerFrame = 200,
			ownActions = actual.ownActions!!,
			conversationName = null,
			sharedActionSequence = null,
			signType = null,
		)
		val rootNode = actual.ownActions!!
		assertNull((rootNode as FixedActionNode).next)
		val action = rootNode.action as ActionTalk
		assertEquals(ActionTargetDialogueObject("Portal"), action.speaker)
		assertEquals("", action.expression)
		assertEquals("It's a Warport Portal!!! Maybe you should get a keychain of one of these to show to your pals?!? That'd be RAD.", action.text)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseValidWarportPortal() {
		val actual = findArea("warport2T3").objects.portals.find { it.x == 6 }!!
		val expected = AreaPortal(
			x = 6, y = 6, destination = TransitionDestination(
				findArea("warport1T2"), null, 36, 4, direction = null
			)
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseBook() {
		val actual = findArea("aeropolis_W_Library").objects.decorations.find { it.x == 1 && it.y == 1 }!!
		val expected = AreaDecoration(
			x = 1,
			y = 1,
			sprites = null,
			canWalkThrough = true,
			light = null,
			timePerFrame = 1,
			ownActions = actual.ownActions!!,
			conversationName = null,
			sharedActionSequence = null,
			signType = null,
		)
		val rootNode = actual.ownActions!!
		assertNotNull((rootNode as FixedActionNode).next)
		var action = rootNode.action as ActionTalk
		assertEquals(ActionTargetDialogueObject("Deities: What ARE they?"), action.speaker)
		assertEquals("", action.expression)
		assertTrue(action.text.startsWith("Deities are entities on a higher level of existence than we mere"))

		var lastNode = rootNode.next
		while (true) {
			val next = (lastNode as FixedActionNode).next ?: break
			lastNode = next
		}
		action = lastNode.action as ActionTalk
		assertEquals(ActionTargetDialogueObject("Deities: What ARE they?"), action.speaker)
		assertEquals("", action.expression)
		assertTrue(action.text.startsWith("Deities are non-physical entities,"))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseCanoniaWoodsTransition() {
		val actual = findArea("canonia_woods").objects.transitions.find { it.y == 73 }!!
		val expected = AreaTransition(x = 13, y = 73, arrow = null, destination = TransitionDestination(
			null, content.worldMaps[0], x = 1, y = 1, direction = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseCanoniaEquipmentShop() {
		val actual = findArea("cn_shop_").objects.shops[0]
		val expected = AreaShop(shopName = "Canonia Equipment Shop", x = 2, y = 2, waresConstantName = "CANONIA_EQUIPMENT")
		assertEquals(expected, actual)
	}

	@Test
	fun testMineDiveExamine() {
		val actual = findArea("gemmine6").objects.decorations.find { it.x == 8 }!!
		val expected = AreaDecoration(
			x = 8,
			y = 30,
			sprites = null,
			canWalkThrough = true,
			light = null,
			timePerFrame = 1,
			ownActions = null,
			conversationName = "c_lakeQur",
			sharedActionSequence = null,
			signType = null,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSwitchOrb() {
		val expected = AreaSwitchOrb(
			x = 3, y = 2, color = content.areas.switchColors.find { it.name == "turquoise" }!!
		)
		val actual = findArea("canonia_shaman_d").objects.switchOrbs.find { it.x == 3 }!!
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSwitchGate() {
		val expected = AreaSwitchGate(
			x = 12, y = 14, color = content.areas.switchColors.find { it.name == "moonstone" }!!
		)
		val actual = findArea("citadel4").objects.switchGates.find { it.x == 12 }!!
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSwitchPlatform() {
		val expected = AreaSwitchPlatform(
			x = 31, y = 8, color = content.areas.switchColors.find { it.name == "turquoise" }!!
		)
		val actual = findArea("dreamshrine2").objects.switchPlatforms.find { it.x == 31 }!!
		assertEquals(expected, actual)
	}
}
