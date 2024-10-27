package mardek.importer.area

import mardek.assets.area.Direction
import mardek.assets.area.TransitionDestination
import mardek.assets.area.objects.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestAreaEntityParser {

	@Test
	fun testSingleEntitySingleKey() {
		val rawEntities = "[{name:\"Guard\"}]"
		val parsed1 = parseAreaEntities1(rawEntities)
		assertEquals(1, parsed1.size)
		assertEquals(1, parsed1[0].size)
		assertEquals("\"Guard\"", parsed1[0]["name"])
	}

	@Test
	fun testSingleEntityTwoKeys() {
		val rawEntities = "[{name:\"Guard\",x:22}]"
		val parsed1 = parseAreaEntities1(rawEntities)
		println(parsed1)
		assertEquals(1, parsed1.size)
		assertEquals(2, parsed1[0].size)
		assertEquals("\"Guard\"", parsed1[0]["name"])
		assertEquals("22", parsed1[0]["x"])
	}

	@Test
	fun testTwoEntitiesWithSingleKey() {
		val rawEntities = "[{x:1},{y:2}]"
		val parsed1 = parseAreaEntities1(rawEntities)
		assertEquals(2, parsed1.size)
		assertEquals(1, parsed1[0].size)
		assertEquals("1", parsed1[0]["x"])
		assertEquals(1, parsed1[1].size)
		assertEquals("2", parsed1[1]["y"])
	}

	@Test
	fun testTwoEntitiesWithTwoKeys() {
		val rawEntities = "[{x:1,y:2},{x:3,y:4}]"
		val parsed1 = parseAreaEntities1(rawEntities)
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
		val parsed1 = parseAreaEntities1(rawEntities)
		assertEquals(1, parsed1.size)
		assertEquals(8, parsed1[0].size)
		assertEquals("\"AIR\"", parsed1[0]["elem"])

		val expectedConversation = "[[\"sour\",\"This is the Air Temple. You may enter to pay your respects to the Air Crystal " +
				"or to receive blessings from the Priestess, but the deeper sections of the temple are off-limits " +
				"to people of no significance such as yourself.\"]]"
		assertEquals(expectedConversation, parsed1[0]["conv"])
	}

	private fun parseAreaEntityRaw(rawString: String): Any {
		val parsedEntities = parseAreaEntities("[$rawString]")
		assertEquals(1, parsedEntities.size)
		return parsedEntities[0]
	}

	@Test
	fun testParseTransitionWithArrowWithoutDirection() {
		val actual = parseAreaEntityRaw(
			"{name:\"EXIT\",model:\"area_transition\",x:3,y:7,dest:[\"aeropolis_E\",22,24],ARROW:\"S\"}"
		)
		val expected = AreaTransition(x = 3, y = 7, arrow = "S", destination = TransitionDestination(
			areaName = "aeropolis_E",
			x = 22,
			y = 24,
			direction = null
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseTransitionWithoutArrowWithDirection() {
		val actual = parseAreaEntityRaw(
			"{name:\"Item Shop\",model:\"area_transition\",x:22,y:23,dir:\"n\",dest:[\"aeropolis_E_iShop\",3,6]}"
		)
		val expected = AreaTransition(x = 22, y = 23, arrow = null, destination = TransitionDestination(
			areaName = "aeropolis_E_iShop",
			x = 3,
			y = 6,
			direction = Direction.Up
		))
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWalkTriggerSimple() {
		val actual = parseAreaEntityRaw(
			"{name:\"INTERJECTION\",model:\"_trigger\",x:8,y:5,ExecuteScript:function()\n" +
				"{\n" +
				"   _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");\n" +
				"}}")
		val expected = AreaTrigger(
			name = "INTERJECTION",
			x = 8,
			y = 5,
			flashCode = "function()\n{\n   _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");\n}",
			oneTimeOnly = true,
			oncePerAreaLoad = false,
			walkOn = false
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
			"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:8,y:5,triggers:-1,WALKON:true,ExecuteScript:$executeScript}"
		)
		val expected = AreaTrigger(
			name = "TRANSPORT_TRIGGER",
			x = 8,
			y = 5,
			flashCode = executeScript,
			oneTimeOnly = false,
			oncePerAreaLoad = false,
			walkOn = true
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseWalkTriggerInventor() {
		val flashCode = "function(){\tDO_ACTIONS([[\"UNFREEZE\"],[\"TALK\",\"c_inventor\"]],\"PC\",true);}"

		val actual = parseAreaEntityRaw(
			"{name:\"TALK_TRIGGER\",model:\"_trigger\",x:3,y:5,triggers:1,recurring:true,ExecuteScript:$flashCode}"
		)
		val expected = AreaTrigger(
			name = "TALK_TRIGGER",
			x = 3,
			y = 5,
			oneTimeOnly = true,
			oncePerAreaLoad = true,
			flashCode = "function(){\tDO_ACTIONS([[\"UNFREEZE\"],[\"TALK\",\"c_inventor\"]],\"PC\",true);}",
			walkOn = false
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

	@Test
	fun testParseSaveCrystal() {
		val actual = parseAreaEntityRaw(
			"{name:\"Save Crystal\",model:\"o_Crystal\",x:4,y:7,walkspeed:-1,conv:\"c_healingCrystal\"}"
		)
		val expected = AreaObject(
			x = 4,
			y = 7,
			firstFrameIndex = null,
			numFrames = null,
			spritesheetName = "obj_Crystal",
			conversationName = "c_healingCrystal",
			rawConversion = null,
			signType = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSlainDracelon() {
		val actual = parseAreaEntityRaw(
			"{name:\"Dracelon\",model:\"ch2bosses\",x:4,y:21,walkspeed:-1,FRAME:4,silent:true," +
					"Static:true,elem:\"EARTH\",conv:\"c_A_Rohoph\"}"
		)
		val expected = AreaObject(
			spritesheetName = "spritesheet_ch2bosses",
			firstFrameIndex = 4,
			numFrames = 1,
			x = 4,
			y = 21,
			conversationName = "c_A_Rohoph",
			rawConversion = null,
			signType = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseZombieDragon() {
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
		val actual = parseAreaEntityRaw(
			"{name:\"Zombie Dragon\",model:\"dragon\",x:11,y:6,walkspeed:-1,dir:\"n\",Static:1,elem:\"DARK\",conv:[Do = $flashCode]}"
		)
		val expected = AreaObject(
			spritesheetName = "spritesheet_dragon",
			firstFrameIndex = 2,
			numFrames = 2,
			x = 11,
			y = 6,
			conversationName = null,
			rawConversion = "[Do = $flashCode]",
			signType = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseTheDragon() {
		val actual = parseAreaEntityRaw(
			"{name:\"The Dragon\",model:\"dragon\",x:6,y:7,walkspeed:-1,dir:\"s\",elem:\"DARK\"," +
					"conv:[[\"\",\"You shouldn\\'t be able to talk to me! REPORT THIS BUG PLEASE!\"]]}"
		)
		val expected = AreaCharacter(
			name = "The Dragon",
			spritesheetName = "spritesheet_dragon",
			startX = 6,
			startY = 7,
			startDirection = Direction.Down,
			silent = false,
			walkSpeed = -1,
			element = "DARK",
			conversationName = null,
			rawConversation = "[[\"\",\"You shouldn\\'t be able to talk to me! REPORT THIS BUG PLEASE!\"]]",
			encyclopediaPerson = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseMoric() {
		val actual = parseAreaEntityRaw(
			"{name:\"Moric\",model:\"moric\",x:7,y:11,walkspeed:-2,FRAME:1,elem:\"EARTH\",conv:\"c_GdM_Moric\"}"
		)
		val expected = AreaObject(
			spritesheetName = "spritesheet_moric",
			firstFrameIndex = 1,
			numFrames = 1,
			x = 7,
			y = 11,
			conversationName = "c_GdM_Moric",
			rawConversion = null,
			signType = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParsePriestessGail() {
		val actual = parseAreaEntityRaw(
			"{name:\"Priestess Gail\",model:\"priestess\",x:7,y:3,walkspeed:-1,dir:\"s\",elem:\"AIR\"," +
					"conv:\"c_priestess\",EN:[\"People\",\"Priestess Gail\"]}"
		)
		val expected = AreaCharacter(
			name = "Priestess Gail",
			spritesheetName = "spritesheet_priestess",
			startX = 7,
			startY = 3,
			startDirection = Direction.Down,
			silent = false,
			walkSpeed = -1,
			element = "AIR",
			conversationName = "c_priestess",
			rawConversation = null,
			encyclopediaPerson = "Priestess Gail"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseSign() {
		val actual = parseAreaEntityRaw(
			"{name:\"Sign\",model:\"sign\",sign:\"words\",FRAME:10,x:26,y:26,walkspeed:-2," +
					"conv:[[\"\",\"CLOEST FORE THE NYHTE\"]]}"
		)
		val expected = AreaObject(
			spritesheetName = "spritesheet_sign",
			firstFrameIndex = 10,
			numFrames = 1,
			x = 26,
			y = 26,
			conversationName = null,
			rawConversion = "[[\"\",\"CLOEST FORE THE NYHTE\"]]",
			signType = "words"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseBigDoor() {
		val actual = parseAreaEntityRaw(
			"{name:\"UP\",model:\"BIGDOOR3\",x:19,y:13,dest:[\"crypt2\",19,13],dir:\"s\"}"
		)
		val expected = AreaDoor(
			spritesheetName = "BIGDOORSHEET",
			spriteRow = 3,
			x = 19,
			y = 13,
			destination = TransitionDestination(
				areaName = "crypt2",
				x = 19,
				y = 13,
				direction = Direction.Down
			),
			lockType = null,
			keyName = null
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseLockedDoor() {
		val actual = parseAreaEntityRaw(
			"{name:\"Exit\",model:\"DOOR5\",x:4,y:8,lock:\"key\",key:\"Bandit Key\"," +
					"dir:\"s\",dest:[\"aeropolis_E\",13,20]}"
		)
		val expected = AreaDoor(
			spritesheetName = "DOORSHEET",
			spriteRow = 5,
			x = 4,
			y = 8,
			destination = TransitionDestination("aeropolis_E", 13, 20, Direction.Down),
			lockType = "key",
			keyName = "Bandit Key"
		)
		assertEquals(expected, actual)
	}

	@Test
	fun testParseStatue() {
		val actual = parseAreaEntityRaw(
			"{name:\"Statue\",model:\"statue\",x:25,y:3,walkspeed:-2,dir:\"m1\",FRAME:6,conv:[statueFlavour]}"
		)
		val expected = AreaObject(
			spritesheetName = "spritesheet_statue",
			firstFrameIndex = 6,
			numFrames = 1,
			x = 25,
			y = 3,
			conversationName = null,
			rawConversion = "[statueFlavour]",
			signType = null
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
	}

	@Test
	fun testParseDreamCircleTriggerEnter() {
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
	}

	@Test
	fun testParseDreamCircleSpecialEnter() {
		val actual = parseAreaEntityRaw(
			"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:8,y:5,triggers:-1,WALKON:true,ExecuteScript:function()\n" +
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
					"}}"
		)
	}

	@Test
	fun testParseDreamCircleSpecialExit() {
		val actual = parseAreaEntityRaw(
			"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:8,y:5,triggers:-1,WALKON:Boolean(GameData.plotVars.Mardek_itj_dreamcave1),ExecuteScript:function()\n" +
					"{\n" +
					"   if(!GameData.plotVars.Mardek_itj_dreamcave1)\n" +
					"   {\n" +
					"      _root.Interjection(\"Mardek\",\"dreamcave1\",\"c_A_Gloria\");\n" +
					"   }\n" +
					"   else\n" +
					"   {\n" +
					"      _root.WarpTrans([\"canonia_dreamcave\",8,5]);\n" +
					"   }\n" +
					"}}"
		)
	}

	@Test
	fun testParseInvalidWarportPortal() {
		val actual = parseAreaEntityRaw(
			"{name:\"Portal\",model:\"o_portal\",x:36,y:4," +
					"conv:[[\"\",\"We hope you enjoyed your trip. Please leave the arrivals area.\"]]}"
		)
	}

	@Test
	fun testParseValidWarportPortal() {
		val actual = parseAreaEntityRaw(
			"{name:\"Portal\",model:\"o_portal\",x:6,y:6,walkable:true," +
					"conv:[[\"\",\"It\\'s a Warport Portal!!! Maybe you should get a keychain of one of these to show to your pals?!? That\\'d be RAD.\"]]}"
		)
		val actual2 = parseAreaEntityRaw(
			"{name:\"TRANSPORT_TRIGGER\",model:\"_trigger\",x:6,y:6,triggers:-1,WALKON:true,ExecuteScript:function()\n" +
					"{\n" +
					"   _root.WarpTrans([\"warport1T2\",36,4]);\n" +
					"}}"
		)
	}

	// TODO Test portals and dream circles

	// TODO Switches, platforms, gates
}
