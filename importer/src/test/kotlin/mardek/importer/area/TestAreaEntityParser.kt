package mardek.importer.area

import mardek.content.Content
import mardek.content.area.Area
import mardek.content.area.Direction
import mardek.content.area.TransitionDestination
import mardek.content.area.objects.*
import mardek.content.sprite.ArrowSprite
import mardek.content.sprite.DirectionalSprites
import mardek.content.sprite.KimSprite
import mardek.content.sprite.ObjectSprites
import mardek.importer.actions.HardcodedActions
import mardek.importer.audio.importAudioContent
import mardek.importer.particle.importParticleEffects
import mardek.importer.stats.importStatsContent
import mardek.importer.util.parseActionScriptObjectList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class TestAreaEntityParser {

	private val content = Content()
	private val testID = UUID.randomUUID()

	init {
		importAudioContent(content.audio)
		importParticleEffects(content)
		importStatsContent(content)
	}

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
		val transitions = ArrayList<Pair<TransitionDestination, String>>()
		val parsedEntities = parseAreaObjectsToList(
			content, hardcodedActions, areaName,
			"[$rawString]", transitions,
		)
		assertEquals(1, parsedEntities.size)

		for ((transition, destination) in transitions) {
			val area = Area()
			area.properties.rawName = destination
			transition.area = area
			content.areas.areas.add(area)
		}
		return parsedEntities[0]
	}

	private fun assertArea(name: String) = content.areas.areas.find { it.properties.rawName == name }!!

	@Test
	fun testParseTransitionWithArrowWithoutDirection() {
		content.areas.arrowSprites.add(ArrowSprite("S", KimSprite()))
		val actual = parseAreaEntityRaw(
			"{name:\"EXIT\",model:\"area_transition\",x:3,y:7,dest:[\"aeropolis_E\",22,24],ARROW:\"S\"}"
		)
		val expected = AreaTransition(
			x = 3, y = 7, arrow = content.areas.arrowSprites[0], destination = TransitionDestination(
			area = assertArea("aeropolis_E"), x = 22, y = 24, direction = null, discoveredAreaName = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseTransitionWithoutArrowWithDirection() {
		val actual = parseAreaEntityRaw(
			"{name:\"Item Shop\",model:\"area_transition\",x:22,y:23,dir:\"n\",dest:[\"aeropolis_E_iShop\",3,6]}"
		)
		val expected = AreaTransition(x = 22, y = 23, arrow = null, destination = TransitionDestination(
			area = assertArea("aeropolis_E_iShop"), x = 3, y = 6,
			direction = Direction.Up, discoveredAreaName = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWalkTriggerSimple() {
		val actual = parseAreaEntityRaw(
			"{name:\"INTERJECTION\",model:\"_trigger\",x:8,y:5,ExecuteScript:function()\n" +
				"{\n" +
				"   _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");\n" +
				"},uuid:$testID}")
		val expected = AreaTrigger(
			name = "INTERJECTION",
			x = 8,
			y = 5,
			flashCode = "function()\n{\n   _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");\n}",
			oneTimeOnly = true,
			oncePerAreaLoad = false,
			walkOn = false,
			actions = null,
			id = testID,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWalkTriggerDreamCaveCircle() {
		val executeScript = "function()\n" +
				"{\n" +
				"   if(!_root.HasAlly(\"Gloria\") && !HASPLOTITEM(\"Talisman of ONEIROS\"))\n" +
				"   {\n" +
				"      return undefined;\n" +
				"   }\n" +
				"   if(!HASPLOTITEM(\"Talisman of ONEIROS\"))\n" +
				"   {\n" +
				"      MAKEPARTY([\"Mardek\",\"Gloria\",\"Elwyen\",\"Solaar\"],true);\n" +
				"   }\n" +
				"   _root.WarpTrans([\"canonia_dreamcave_d\",8,5]);\n" +
				"}"

		val actual = parseAreaEntityRaw(
			"{uuid:$testID,name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:8,y:5,triggers:-1,WALKON:true,ExecuteScript:$executeScript}"
		)
		val expected = AreaTrigger(
			name = "TRANSPORT_TRIGGER",
			x = 8,
			y = 5,
			flashCode = executeScript,
			oneTimeOnly = false,
			oncePerAreaLoad = false,
			walkOn = true,
			actions = null,
			id = testID,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWalkTriggerInventor() {
		val flashCode = "function(){\tDO_ACTIONS([[\"UNFREEZE\"],[\"TALK\",\"c_inventor\"]],\"PC\",true);}"

		val actual = parseAreaEntityRaw(
			"{uuid:$testID,name:\"TALK_TRIGGER\",model:\"_trigger\",x:3,y:5,triggers:1,recurring:true,ExecuteScript:$flashCode}"
		)
		val expected = AreaTrigger(
			name = "TALK_TRIGGER",
			x = 3,
			y = 5,
			oneTimeOnly = true,
			oncePerAreaLoad = true,
			flashCode = "function(){\tDO_ACTIONS([[\"UNFREEZE\"],[\"TALK\",\"c_inventor\"]],\"PC\",true);}",
			walkOn = false,
			actions = null,
			id = testID,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseTalkTrigger() {
		val actual = parseAreaEntityRaw(
			"{name:\"TALKTRIGGER\",model:\"talktrigger\",x:5,y:2,NPC:\"Shopkeeper\",dir:\"s\"}"
		)
		val expected = AreaTalkTrigger(
			name = "TALKTRIGGER",
			x = 5,
			y = 2,
			npcName = "Shopkeeper"
		)
		assertEquals(expected, actual)
	}

	private fun objectSprite(name: String): ObjectSprites {
		val sprites = ObjectSprites(name, 0, 0, null, emptyArray())
		content.areas.objectSprites.add(sprites)
		return sprites
	}

	private fun characterSprite(name: String): DirectionalSprites {
		val sprites = DirectionalSprites(name, emptyArray())
		content.areas.characterSprites.add(sprites)
		return sprites
	}

	@Test
	fun testParseSaveCrystal() {
		val hardcodedActions = HardcodedActions()
		hardcodedActions.addDummySaveCrystalAction()
		val expected = AreaDecoration(
			x = 4,
			y = 7,
			sprites = objectSprite("obj_Crystal"),
			canWalkThrough = false,
			light = null,
			timePerFrame = 200,
			rawConversation = null,
			conversationName = "c_healingCrystal",
			actionSequence = hardcodedActions.getHardcodedGlobalActionSequence("c_healingCrystal")!!,
			signType = null,
		)
		val actual = parseAreaEntityRaw(
			"{name:\"Save Crystal\",model:\"o_Crystal\",x:4,y:7,walkspeed:-1,conv:\"c_healingCrystal\"}",
			hardcodedActions = hardcodedActions
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSlainDracelon() {
		val id = UUID.randomUUID()
		val expected = AreaCharacter(
			name = "Dracelon",
			directionalSprites = null,
			fixedSprites = objectSprite("spritesheet_ch2bosses(4, 1)"),
			startX = 4,
			startY = 21,
			startDirection = Direction.Down,
			walkSpeed = -2,
			element = content.stats.elements.find { it.rawName == "EARTH" }!!,
			portrait = null,
			conversationName = "c_A_Rohoph",
			rawConversation = null,
			actionSequence = null,
			encyclopediaPerson = null,
			id = id,
		)
		val actual = parseAreaEntityRaw(
				"{name:\"Dracelon\",model:\"ch2bosses\",x:4,y:21,walkspeed:-1,FRAME:4,silent:true," +
						"Static:true,elem:\"EARTH\",conv:\"c_A_Rohoph\",uuid:$id}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseZombieDragon() {
		val id = UUID.randomUUID()
		val flashCode = "function()\n" +
				"   {\n" +
				"      _root.playSFX(\"dragon_roar\");\n" +
				"      return 1;\n" +
				"   },[\"angr\",\"Rrrrrrrrrrrrrrrrrrrrr!!!!\"],Do = function()\n" +
				"   {\n" +
				"      GameData.plotVars.ZDRAGON = 1;\n" +
				"      BATTLE([[\"ZombieDragon\",null,null,null],[\"Zombie Dragon\",null,null,null],[18,null,null,null],\"DRAGON\"],\"BossBattle\",true);\n" +
				"      return true;\n" +
				"   }"
		val expected = AreaCharacter(
			name = "Zombie Dragon",
			directionalSprites = null,
			fixedSprites = objectSprite("spritesheet_dragon(2, 2)"),
			startX = 11,
			startY = 6,
			startDirection = Direction.Down,
			walkSpeed = -2,
			element = content.stats.elements.find { it.rawName == "DARK" }!!,
			portrait = null,
			conversationName = null,
			rawConversation = "[Do = $flashCode]",
			actionSequence = null,
			encyclopediaPerson = null,
			id = id,
		)
		val actual = parseAreaEntityRaw(
				"{name:\"Zombie Dragon\",model:\"dragon\",x:11,y:6,walkspeed:-1,dir:\"n\",Static:1," +
						"elem:\"DARK\",conv:[Do = $flashCode],uuid:$id}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseTheDragon() {
		val id = UUID.fromString("6d8a7f59-5b45-4054-8266-49eae259fdbb")
		val expected = AreaCharacter(
			name = "The Dragon",
			directionalSprites = null,
			fixedSprites = objectSprite("spritesheet_dragon(0, 2)"),
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
		val actual = parseAreaEntityRaw(
				"{name:\"The Dragon\",model:\"dragon\",x:6,y:7,walkspeed:-1,dir:\"s\",elem:\"DARK\"," +
						"Static:1,conv:[],uuid:6d8a7f59-5b45-4054-8266-49eae259fdbb}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseMoric() {
		val id = UUID.randomUUID()
		val expected = AreaCharacter(
			name = "Moric",
			directionalSprites = null,
			fixedSprites = objectSprite("spritesheet_moric(1, 1)"),
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
			id = id,
		)
		val actual = parseAreaEntityRaw(
				"{name:\"Moric\",model:\"moric\",x:7,y:11,walkspeed:-2,FRAME:1,elem:\"EARTH\",conv:\"c_GdM_Moric\",uuid:$id}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParsePriestessGail() {
		val id = UUID.randomUUID()
		val expected = AreaCharacter(
			name = "Priestess Gail",
			directionalSprites = characterSprite("priestess"),
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
			id = id,
		)
		val actual = parseAreaEntityRaw(
				"{name:\"Priestess Gail\",model:\"priestess\",x:7,y:3,walkspeed:-1,dir:\"s\",elem:\"AIR\"," +
						"conv:\"c_priestess\",EN:[\"People\",\"Priestess Gail\"],uuid:$id}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSign() {
		val id = UUID.randomUUID()
		val expected = AreaDecoration(
			x = 26,
			y = 26,
			sprites = objectSprite("spritesheet_sign(10, 1)"),
			canWalkThrough = false,
			light = null,
			timePerFrame = 1,
			conversationName = null,
			rawConversation = "[[\"\",\"CLOEST FORE THE NYHTE\"]]",
			actionSequence = null,
			signType = "words",
		)
		val actual = parseAreaEntityRaw(
				"{name:\"Sign\",model:\"sign\",sign:\"words\",FRAME:10,x:26,y:26,walkspeed:-2," +
						"conv:[[\"\",\"CLOEST FORE THE NYHTE\"]],uuid:$id}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseBigDoor() {
		val sprite = objectSprite("BIGDOOR3")
		val actual = parseAreaEntityRaw(
				"{name:\"UP\",model:\"BIGDOOR3\",x:19,y:13,dest:[\"crypt2\",19,13],dir:\"s\"}"
		)
		val expected = AreaDoor(
			sprites = sprite,
			x = 19,
			y = 13,
			destination = TransitionDestination(
				area = assertArea("crypt2"), x = 19, y = 13,
				direction = Direction.Down, discoveredAreaName = null
			),
			lockType = null,
			keyName = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseLockedDoor() {
		val sprite = objectSprite("DOOR5")
		val actual = parseAreaEntityRaw(
				"{name:\"Exit\",model:\"DOOR5\",x:4,y:8,lock:\"key\",key:\"Bandit Key\"," +
						"dir:\"s\",dest:[\"aeropolis_E\",13,20]}"
		)
		val expected = AreaDoor(
			sprites = sprite,
			x = 4,
			y = 8,
			destination = TransitionDestination(
					assertArea("aeropolis_E"), 13, 20, Direction.Down, null
			),
			lockType = "key",
			keyName = "Bandit Key"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseStatue() {
		val id = UUID.randomUUID()
		val expected = AreaCharacter(
			name = "Statue",
			directionalSprites = null,
			fixedSprites = objectSprite("spritesheet_statue(6, 1)"),
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
			id = id,
		)
		val actual = parseAreaEntityRaw(
				"{name:\"Statue\",model:\"statue\",x:25,y:3,walkspeed:-2,dir:\"m1\",FRAME:6,conv:[statueFlavour],uuid:$id}"
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
		val actual = parseAreaEntityRaw(
			"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:23,y:61,triggers:-1,WALKON:true,ExecuteScript:function()\n" +
					"{\n" +
					"   _root.ExitDreamrealm();\n" +
					"   _root.WarpTrans([\"canonia_woods\",23,61]);\n" +
					"}}"
		)
		val expected = AreaPortal(x = 23, y = 61, destination = TransitionDestination(
			assertArea("canonia_woods"), 23, 61, direction = null, discoveredAreaName = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleEnterCanoniaWoods() {
		val actual = parseAreaEntityRaw(
			"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:23,y:61,triggers:-1,WALKON:true,ExecuteScript:function()\n" +
					"{\n" +
					"   if(!CanEnterDreamrealm())\n" +
					"   {\n" +
					"      return undefined;\n" +
					"   }\n" +
					"   _root.EnterDreamrealm();\n" +
					"   _root.WarpTrans([\"canonia_woods_d\",23,61]);\n" +
					"}}"
		)
		val expected = AreaPortal(x = 23, y = 61, destination = TransitionDestination(
			assertArea("canonia_woods_d"), 23, 61, direction = null, discoveredAreaName = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleEnterTaintedGrotto() {
		val actual = parseAreaEntityRaw(
				"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:33,y:24,triggers:-1,WALKON:true,ExecuteScript:function()\n" +
						"{\n" +
						"   if(!HASPLOTITEM(\"Talisman of ONEIROS\"))\n" +
						"   {\n" +
						"      return undefined;\n" +
						"   }\n" +
						"   _root.WarpTrans([\"pcave3_d\",33,24]);\n" +
						"}}"
		)
		val expected = AreaPortal(x = 33, y = 24, destination = TransitionDestination(
				assertArea("pcave3_d"), 33, 24, direction = null, discoveredAreaName = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleSpecialEnter() {
		val flashCode = "function()\n" +
		"{\n" +
				"   if(!_root.HasAlly(\"Gloria\") && !HASPLOTITEM(\"Talisman of ONEIROS\"))\n" +
				"   {\n" +
				"      return undefined;\n" +
				"   }\n" +
				"   if(!HASPLOTITEM(\"Talisman of ONEIROS\"))\n" +
				"   {\n" +
				"      MAKEPARTY([\"Mardek\",\"Gloria\",\"Elwyen\",\"Solaar\"],true);\n" +
				"   }\n" +
				"   _root.WarpTrans([\"canonia_dreamcave_d\",8,5]);\n" +
				"}"
		val actual = parseAreaEntityRaw(
			"{uuid:$testID,name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:8,y:5,triggers:-1,WALKON:true,ExecuteScript:$flashCode}"
		)
		val expected = AreaTrigger(
			name = "TRANSPORT_TRIGGER",
			x = 8,
			y = 5,
			flashCode = flashCode,
			oneTimeOnly = false,
			oncePerAreaLoad = false,
			walkOn = true,
			actions = null,
			id = testID,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseDreamCircleSpecialExit() {
		val flashCode = "function()\n" +
				"{\n" +
				"   if(!GameData.plotVars.Mardek_itj_dreamcave1)\n" +
				"   {\n" +
				"      _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");\n" +
				"   }\n" +
				"   else\n" +
				"   {\n" +
				"      _root.WarpTrans([\"canonia_dreamcave\",8,5]);\n" +
				"   }\n" +
				"}"
		val actual = parseAreaEntityRaw(
			"{uuid:$testID,name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:8,y:5,triggers:-1,WALKON:Boolean(GameData.plotVars.Mardek_itj_dreamcave1),ExecuteScript:$flashCode}"
		)
		val expected = AreaTrigger(
			name = "TRANSPORT_TRIGGER",
			x = 8,
			y = 5,
			flashCode = flashCode,
			oneTimeOnly = false,
			oncePerAreaLoad = false,
			walkOn = null,
			actions = null,
			id = testID,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseInvalidWarportPortal() {
		val expected = AreaDecoration(
			sprites = objectSprite("obj_portal"),
			x = 36,
			y = 4,
			canWalkThrough = false,
			light = null,
			timePerFrame = 200,
			conversationName = null,
			rawConversation = "[[\"\",\"We hope you enjoyed your trip. Please leave the arrivals area.\"]]",
			actionSequence = null,
			signType = null,
		)
		val actual = parseAreaEntityRaw(
				"{name:\"Portal\",model:\"o_portal\",x:36,y:4," +
						"conv:[[\"\",\"We hope you enjoyed your trip. Please leave the arrivals area.\"]]}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWarportExamine() {
		val expected = AreaDecoration(
			x = 6,
			y = 6,
			sprites = objectSprite("obj_portal"),
			canWalkThrough = true,
			light = null,
			timePerFrame = 200,
			conversationName = null,
			rawConversation = "[[\"\",\"It\\'s a Warport Portal!!! Maybe you should get a keychain of one of these to show to your pals?!? That\\'d be RAD.\"]]",
			actionSequence = null,
			signType = null,
		)
		val actual = parseAreaEntityRaw(
			"{name:\"Portal\",model:\"o_portal\",x:6,y:6,walkable:true," +
					"conv:[[\"\",\"It\\'s a Warport Portal!!! Maybe you should get a keychain of one of these to show to your pals?!? That\\'d be RAD.\"]]}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseValidWarportPortal() {
		val actual = parseAreaEntityRaw(
			"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:6,y:6,triggers:-1,WALKON:true,ExecuteScript:function()\n" +
					"{\n" +
					"   _root.WarpTrans([\"warport1T2\",36,4]);\n" +
					"}}"
		)
		val expected = AreaPortal(
			x = 6, y = 6, destination = TransitionDestination(
				assertArea("warport1T2"), 36, 4, direction = null, discoveredAreaName = null
			)
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseBook() {
		val rawConversation = "[[\"\",\"Deities are entities on a higher level of existence than we mere mortals. " +
				"They are the overseers of the universe; eternal benefactors who watch, maintain and create " +
				"the world and creatures around us. They come in three major types.\"],[\"\",\"There are the " +
				"Higher Creator Deities, such as YALORT, who design creatures and worlds, shaping the elements " +
				"of the universe to their desires. They cannot cause anything to appear; they can merely mould " +
				"what already exists.\"],[\"\",\"The Mid-Level Elemental Deities represent and have control over " +
				"one single element in particular. They have supreme power over that element, so they are the " +
				"ones that grant its use to the Creators. They are the makers of the crystals.\"],[\"\",\"The " +
				"Lesser Archetype Deities are formed from minds, filling niches that society needs to be fulfilled. " +
				"They represent standards of what people should be (and as such they are not omnipotent or perfect, " +
				"\\'out of reach\\'), or they exist as things to worship to for certain specific needs" +
				".\"],[\"\",\"The names of deities should always be fully capitalised. However, the words to " +
				"refer to their followers do not follow this rule; YALORT\\'s followers are Yalortians, for " +
				"example, not YALORTians or YALORTIANS. This is because the capitals show the power of the " +
				"deity\\'s name; derived forms are no longer the name of the deity and lose their power" +
				".\"],[\"\",\"Deities are non-physical entities, and are as such formless. However, when " +
				"they interact, they tend to manifest as the same forms in order to be recognised. As most " +
				"people have never seen a deity, though, any artwork depicting them is speculative; " +
				"a way of putting such a floaty idea into something we can comprehend and recognise.\"]]"
		val actual = parseAreaEntityRaw(
			"{name:\"Deities: What ARE they?\",model:\"object\",x:1,y:1,type:\"examine\",conv:$rawConversation}"
		)
		val expected = AreaDecoration(
			x = 1,
			y = 1,
			sprites = null,
			canWalkThrough = true,
			light = null,
			timePerFrame = 1,
			rawConversation = rawConversation,
			conversationName = null,
			actionSequence = null,
			signType = null,
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseCanoniaWoodsTransition() {
		val actual = parseAreaEntityRaw(
			"{name:\"Cave\",model:\"area_transition\",x:13,y:73,dest:[\"WORLDMAP\",1,1,\"pcave1\"]}"
		)
		val expected = AreaTransition(x = 13, y = 73, arrow = null, destination = TransitionDestination(
			assertArea("WORLDMAP"), x = 1, y = 1, direction = null, discoveredAreaName = "pcave1"
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseCanoniaEquipmentShop() {
		val actual = parseAreaEntityRaw(
			"{name:\"Equipment Shop\",model:\"shop\",x:2,y:2,SHOP:{name:\"Canonia Equipment Shop\",wares:DefaultShops.CANONIA_EQUIPMENT}}"
		)
		val expected = AreaShop(shopName = "Canonia Equipment Shop", x = 2, y = 2, waresConstantName = "CANONIA_EQUIPMENT")
		assertEquals(expected, actual)
	}

	@Test
	fun testMineDiveExamine() {
		val actual = parseAreaEntityRaw(
			"{name:\"Water\",model:\"examine\",x:8,y:30,walkspeed:-1,conv:\"c_lakeQur\"}"
		)
		val expected = AreaDecoration(
			x = 8,
			y = 30,
			sprites = null,
			canWalkThrough = true,
			light = null,
			timePerFrame = 1,
			rawConversation = null,
			conversationName = "c_lakeQur",
			actionSequence = null,
			signType = null,
		)
		assertEquals(expected, actual)
	}

	private fun switchColor(name: String): SwitchColor {
		val color = SwitchColor(
			name, KimSprite(), KimSprite(),
			KimSprite(), KimSprite(), UUID.randomUUID(),
		)
		content.areas.switchColors.add(color)
		return color
	}

	@Test
	fun testParseSwitchOrb() {
		val expected = AreaSwitchOrb(x = 3, y = 2, color = switchColor("turquoise"))
		val actual = parseAreaEntityRaw(
			"{name:\"Turquoise Keystone\",model:\"object\",x:3,y:2,type:\"switch_orb\"," +
					"colour:\"turquoise\",base:\"gold\",conv_action:1}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSwitchGate() {
		val expected = AreaSwitchGate(x = 12, y = 14, color = switchColor("moonstone"))
		val actual = parseAreaEntityRaw(
			"{name:\"Moonstone Gate\",model:\"object\",x:12,y:14,type:\"switch_gate\",colour:\"moonstone\"}"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSwitchPlatform() {
		val expected = AreaSwitchPlatform(x = 31, y = 8, color = switchColor("turquoise"))
		val actual = parseAreaEntityRaw(
			"{name:\"Turquoise Platform\",model:\"object\",x:31,y:8,type:\"switch_platform\",colour:\"turquoise\"}"
		)
		assertEquals(expected, actual)
	}
}
