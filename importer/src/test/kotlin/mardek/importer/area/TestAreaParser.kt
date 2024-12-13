package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.assets.area.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestAreaParser {

	@Test
	fun testParseAeropolisNorth() {
		val parsed = parseArea(AreaAssets(), "aeropolis_N", ArrayList(), ArrayList())
		assertEquals("aeropolis_N", parsed.properties.rawName)
		assertEquals("Aeropolis - Temple District", parsed.properties.displayName)
		assertEquals("aeropolis", parsed.tilesheet.name)
		assertEquals(47, parsed.width)
		assertEquals(42, parsed.height)
		assertEquals(10, parsed.getTileId(0, 0))
		assertEquals(12, parsed.getTileId(0, 41))
		assertNull(parsed.randomBattles)
		assertNull(parsed.properties.musicTrack) // Special case because of Elwyen date
		assertNull(parsed.properties.dungeon)
		assertSame(AreaAmbience.GENERIC_EXTERNAL_AMBIENCE, parsed.properties.ambience)
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
		assertEquals("Aeropolis", parsed.properties.encyclopediaName)
		assertEquals(AreaDreamType.None, parsed.properties.dreamType)
		assertEquals(AreaChestType.Default, parsed.properties.chestType)
		assertEquals(AreaSnowType.None, parsed.properties.snowType)

		assertEquals(15, parsed.objects.transitions.size)
		assertEquals(1, parsed.objects.walkTriggers.size)
		assertEquals(13, parsed.objects.characters.size)
	}

	@Test
	fun testParseDragonLairArea2() {
		val parsed = parseArea(AreaAssets(), "DL_area2", ArrayList(), ArrayList())
		assertEquals("DL_area2", parsed.properties.rawName)
		assertEquals("Dragon's Lair", parsed.properties.displayName)
		assertEquals("dragonlair", parsed.tilesheet.name)
		assertEquals(15, parsed.width)
		assertEquals(40, parsed.height)
		assertEquals(RandomAreaBattles(
			ownEnemies = arrayListOf(
				BattleEnemySelection("SOLO", arrayListOf("monster", null, null, null)),
				BattleEnemySelection("DUO", arrayListOf("monster", "monster", null, null)),
				BattleEnemySelection("TRIO", arrayListOf("monster", "monster", "monster", null))
			),
			monstersTableName = null,
			levelRangeName = "DRAGON_LAIR",
			ownLevelRange = null,
			minSteps = 0,
			chance = 10,
			specialBackground = null
		), parsed.randomBattles)
		assertEquals("MightyHeroes", parsed.properties.musicTrack)
		assertEquals("DragonLair", parsed.properties.dungeon)
		assertNull(parsed.properties.ambience)
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
		assertNull(parsed.properties.encyclopediaName)
		assertEquals(AreaDreamType.None, parsed.properties.dreamType)
		assertEquals(AreaChestType.Default, parsed.properties.chestType)
		assertEquals(AreaSnowType.None, parsed.properties.snowType)
		assertEquals(2, parsed.objects.doors.size)
		assertEquals(1, parsed.objects.walkTriggers.size)

		assertEquals(10, parsed.objects.decorations.count { it.sprites?.flashName == "torch(2, 16)" })
		assertEquals(10, parsed.objects.decorations.count { it.sprites?.flashName == "SkullBrazier(0, 32)" })
	}

	@Test
	fun testParseGoldfishWarp() {
		val parsed = parseArea(AreaAssets(), "goldfish_warp", ArrayList(), ArrayList())
		assertEquals(AreaAmbience(rgba(16, 14, 10, 100), 0), parsed.properties.ambience)
	}

	@Test
	fun testParseAirTemple() {
		val parsed = parseArea(AreaAssets(), "aeropolis_N_TAIR", ArrayList(), ArrayList())
		assertEquals(1, parsed.objects.transitions.size)
		assertEquals(1, parsed.objects.objects.size)
		assertEquals(1, parsed.objects.characters.size)
		assertEquals("Aeropolis", parsed.properties.musicTrack)
		assertTrue(parsed.flags.hasClearMap)
		// TODO Add error support
	}

	@Test
	fun testParseTheatre() {
		val parsed = parseArea(AreaAssets(), "aeropolis_W_theatre", ArrayList(), ArrayList())
		assertEquals(20, parsed.objects.characters.size)
	}

	@Test
	fun testParseGuardPost() {
		val parsed = parseArea(AreaAssets(), "guardpost", ArrayList(), ArrayList())
		assertEquals(2, parsed.objects.characters.size)
		assertEquals(2, parsed.objects.transitions.size)
	}

	@Test
	fun testEnumerateAreas() {
		assertEquals(258, enumerateAreas().size)
	}

//	@Test
//	fun testParseAndRegisterAllAreasWithoutErrors() {
//		// TODO Stop this hack
//
//		val outputFolder = File("../game/src/main/resources/mardek/game/")
//		assertNull(assets.playableCharacters[0].areaSheet.indices)
//		val renderOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/area-assets.bin").toPath()))
//		sprites.writeRenderData(renderOutput)
//		renderOutput.close()
//		assertNotNull(assets.playableCharacters[0].areaSheet.indices)
//
//		val bitOutput = BitOutputStream(BufferedOutputStream(Files.newOutputStream(File("$outputFolder/areas.bin").toPath())))
//		val bitser = Bitser(false)
//		bitser.serialize(assets, bitOutput)
//		bitOutput.finish()
//
//		val bitInput = BitInputStream(BufferedInputStream(Files.newInputStream(File("$outputFolder/areas.bin").toPath())))
//		val loaded = bitser.deserialize(Campaign::class.java, bitInput)
//		bitInput.close()
//
//		assertEquals(assets.areas.size, loaded.areas.size)
//		assertEquals(assets.areas.sumOf { it.width }, loaded.areas.sumOf { it.width })
//	}
}
