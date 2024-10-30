package mardek.importer.area

import mardek.assets.area.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestAreaParser {

	@Test
	fun testParseAeropolisNorth() {
		val parsed = parseArea("aeropolis_N")
		assertEquals("aeropolis_N", parsed.rawName)
		assertEquals("Aeropolis - Temple District", parsed.displayName)
		assertEquals("aeropolis", parsed.tilesheetName)
		assertEquals(47, parsed.width)
		assertEquals(42, parsed.height)
		assertEquals(10, parsed.getTileId(0, 0))
		assertEquals(12, parsed.getTileId(0, 41))
		assertNull(parsed.randomBattles)
		assertNull(parsed.musicTrack) // Special case because of Elwyen date
		assertNull(parsed.dungeon)
		assertSame(AreaAmbience.GENERIC_EXTERNAL_AMBIENCE, parsed.ambience)
		assertEquals(AreaFlags(
			canWarp = true,
			hasClearMap = true,
			noMovingCamera = false,
			hideParty = false,
			noSwitch = false,
			noMap = false,
			miasma = false,
			noStorage = false
		), parsed.flags)
		assertEquals("Aeropolis", parsed.encyclopediaName)
		assertEquals(AreaDreamType.None, parsed.dreamType)
		assertEquals(AreaChestType.Default, parsed.chestType)
		assertEquals(AreaSnowType.None, parsed.snowType)
	}

	@Test
	fun testParseDragonLairArea2() {
		val parsed = parseArea("DL_area2")
		assertEquals("DL_area2", parsed.rawName)
		assertEquals("Dragon's Lair", parsed.displayName)
		assertEquals("dragonlair", parsed.tilesheetName)
		assertEquals(15, parsed.width)
		assertEquals(40, parsed.height)
		assertEquals(RandomAreaBattles(
			ownEnemies = listOf(
				BattleEnemySelection("SOLO", listOf("monster", null, null, null)),
				BattleEnemySelection("DUO", listOf("monster", "monster", null, null)),
				BattleEnemySelection("TRIO", listOf("monster", "monster", "monster", null))
			),
			monstersTableName = null,
			levelRangeName = "DRAGON_LAIR",
			ownLevelRange = null,
			minSteps = 0,
			chance = 10,
			specialBackground = null
		), parsed.randomBattles)
		assertEquals("MightyHeroes", parsed.musicTrack)
		assertEquals("DragonLair", parsed.dungeon)
		assertNull(parsed.ambience)
		assertEquals(AreaFlags(
			canWarp = false,
			hasClearMap = true,
			noMovingCamera = false,
			hideParty = false,
			noSwitch = false,
			noMap = false,
			miasma = false,
			noStorage = true
		), parsed.flags)
		assertNull(parsed.encyclopediaName)
		assertEquals(AreaDreamType.None, parsed.dreamType)
		assertEquals(AreaChestType.Default, parsed.chestType)
		assertEquals(AreaSnowType.None, parsed.snowType)
	}
}
