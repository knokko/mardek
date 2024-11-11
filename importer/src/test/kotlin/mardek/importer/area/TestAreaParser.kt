package mardek.importer.area

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.assets.GameAssets
import mardek.assets.PlayableCharacter
import mardek.assets.area.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files

class TestAreaParser {

	@Test
	fun testParseAeropolisNorth() {
		val parsed = parseArea("aeropolis_N")
		assertEquals("aeropolis_N", parsed.properties.rawName)
		assertEquals("Aeropolis - Temple District", parsed.properties.displayName)
		assertEquals("aeropolis", parsed.tilesheetName)
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
		val parsed = parseArea("DL_area2")
		assertEquals("DL_area2", parsed.properties.rawName)
		assertEquals("Dragon's Lair", parsed.properties.displayName)
		assertEquals("dragonlair", parsed.tilesheetName)
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

		assertEquals(10, parsed.objects.decorations.count { it.spritesheetName == "torch" })
		assertEquals(10, parsed.objects.decorations.count { it.spritesheetName == "SkullBrazier" })
	}

	@Test
	fun testParseGoldfishWarp() {
		val parsed = parseArea("goldfish_warp")
		assertEquals(AreaAmbience(rgba(16, 14, 10, 100), 0), parsed.properties.ambience)
	}

	@Test
	fun testParseAirTemple() {
		val parsed = parseArea("aeropolis_N_TAIR")
		assertEquals(1, parsed.objects.transitions.size)
		assertEquals(1, parsed.objects.objects.size)
		assertEquals(1, parsed.objects.characters.size)
		assertEquals("Aeropolis", parsed.properties.musicTrack)
		assertTrue(parsed.flags.hasClearMap)
		// TODO Add error support
	}

	@Test
	fun testParseTheatre() {
		val parsed = parseArea("aeropolis_W_theatre")
		assertEquals(20, parsed.objects.characters.size)
	}

	@Test
	fun testParseGuardPost() {
		val parsed = parseArea("guardpost")
		assertEquals(2, parsed.objects.characters.size)
		assertEquals(2, parsed.objects.transitions.size)
	}

	@Test
	fun testEnumerateAreas() {
		assertEquals(258, enumerateAreas().size)
	}

	@Test
	fun testParseAndRegisterAllAreasWithoutErrors() {
		val optimizedAreas = ArrayList<OptimizedArea>()
		val sprites = AreaSprites()
		var totalArea = 0
		for (areaName in enumerateAreas()) {
			val area = parseArea(areaName)
			totalArea += area.width * area.height
			optimizedAreas.add(sprites.register(area))
		}
		println("total area is $totalArea")

		val assets = GameAssets(optimizedAreas, arrayListOf(
			PlayableCharacter(sprites.getCharacter("mardek_hero")),
			PlayableCharacter(sprites.getCharacter("deugan_hero"))
		))
		val startTime = System.nanoTime()

		val outputFolder = File("../game/src/main/resources/mardek/game/")
		val bitOutput = BitOutputStream(BufferedOutputStream(Files.newOutputStream(File("$outputFolder/areas.bin").toPath())))
		val bitser = Bitser(false)
		bitser.serialize(assets, bitOutput)
		bitOutput.finish()

		val midTime1 = System.nanoTime()
		println("encoding took ${(midTime1 - startTime) / 1000_000} ms")

		val bitInput = BitInputStream(BufferedInputStream(Files.newInputStream(File("$outputFolder/areas.bin").toPath())))
		val loaded = bitser.deserialize(GameAssets::class.java, bitInput)
		bitInput.close()

		assertEquals(assets.areas.size, loaded.areas.size)
		assertEquals(assets.areas.sumOf { it.width }, loaded.areas.sumOf { it.width })

		val midTime2 = System.nanoTime()
		println("decoding took ${(midTime2 - midTime1) / 1000_000} ms")

		val renderOutput = BufferedOutputStream(Files.newOutputStream(File("$outputFolder/area-assets.bin").toPath()))
		sprites.writeRenderData(renderOutput)
		renderOutput.close()

		val endTime = System.nanoTime()
		println("saving render data took ${(endTime - midTime2) / 1000_000} ms")
	}
}
