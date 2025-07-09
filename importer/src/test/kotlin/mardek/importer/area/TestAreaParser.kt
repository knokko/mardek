package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.Content
import mardek.content.animation.CombatantAnimations
import mardek.content.area.*
import mardek.content.battle.PartyLayoutPosition
import mardek.content.inventory.ItemStack
import mardek.importer.audio.importAudioContent
import mardek.importer.battle.importBattleContent
import mardek.importer.battle.importMonsterStats
import mardek.importer.stats.importStatsContent
import mardek.importer.inventory.importItemsContent
import mardek.importer.particle.importParticleEffects
import mardek.importer.skills.importSkillsContent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val MONSTER_PROPERTIES_TEXT = """
mdlStats = {names:["Monster"],model:"monster",sprite:"monster",Class:"Monster",TYPE:"BEAST",cElem:"DARK",baseStats:{hp:1200,mp:10,STR:30,VIT:24,SPR:2,AGL:8},nAtk:50,nDef:0,nMDef:0,critical:3,evasion:0,hpGrowth:28,atkGrowth:[0,0],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["none"],accs:["none"],accs2:["none"]},resist:{DARK:30,LIGHT:-100,PSN:0,PAR:0,DRK:0,CNF:0,NUM:0,SIL:0,CRS:0,SLP:0,ZOM:0,BSK:0,BLD:0},EXP:300};
Techs = [{skill:"Dark Claw",type:"ACT",DMG:["m",1.5],MP:10,critical:10,accuracy:100,stfx:{DRK:10},AP:0,MODE:"P",elem:"DARK",TT:"SINGLE",pfx:"darkclaw"}];
Gambits = [{command:"Dark Claw",target:"ANY_PC",criteria:["random",30]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["Monster Fang",20]];
DetermineStats();
"""

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestAreaParser {

	private val content = Content()

	init {
		importAudioContent(content.audio)
		importParticleEffects(content)
		importStatsContent(content)
		importSkillsContent(content)
		importItemsContent(content)
		importBattleContent(content, null)

		content.battle.monsters.add(importMonsterStats(
			name = "monster", animations = CombatantAnimations(), propertiesText = MONSTER_PROPERTIES_TEXT, content
		))
		content.areas.enemySelections.add(SharedEnemySelections(name = "TAINTED_GROTTO", selections = ArrayList()))
		importAreaContent(content)
	}

	@Test
	fun testParseAeropolisNorth() {
		val parsed = parseArea(content, "aeropolis_N", ArrayList(), ArrayList())
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
		assertEquals(AreaSnowType.None, parsed.properties.snowType)

		assertEquals(15, parsed.objects.transitions.size)
		assertEquals(1, parsed.objects.walkTriggers.size)
		assertEquals(13, parsed.objects.characters.size)
	}

	@Test
	fun testParseDragonLairArea2() {
		val solo = content.battle.enemyPartyLayouts.find { it.name == "SOLO" }!!
		val duo = content.battle.enemyPartyLayouts.find { it.name == "DUO" }!!
		val trio = content.battle.enemyPartyLayouts.find { it.name == "TRIO" }!!
		val empty = PartyLayoutPosition(0, 0)
		assertArrayEquals(arrayOf(PartyLayoutPosition(60, 80), empty, empty, empty), solo.positions)
		assertArrayEquals(arrayOf(
			PartyLayoutPosition(56, 46), PartyLayoutPosition(56, 140), empty, empty
		), duo.positions)
		assertArrayEquals(arrayOf(
			PartyLayoutPosition(52, 90), PartyLayoutPosition(22, 154), PartyLayoutPosition(30, 18), empty
		), trio.positions)

		val levelRange = content.areas.levelRanges.find { it.name == "DRAGON_LAIR" }!!
		assertEquals(LevelRange(30, 36), levelRange.range)

		val monster = content.battle.monsters.find { it.name == "monster" }!!

		val parsed = parseArea(content, "DL_area2", ArrayList(), ArrayList())
		assertEquals("DL_area2", parsed.properties.rawName)
		assertEquals("Dragon's Lair", parsed.properties.displayName)
		assertEquals("dragonlair", parsed.tilesheet.name)
		assertEquals(15, parsed.width)
		assertEquals(40, parsed.height)
		assertEquals(RandomAreaBattles(
			ownEnemies = arrayListOf(
				BattleEnemySelection(arrayListOf(monster, null, null, null), solo),
				BattleEnemySelection(arrayListOf(monster, monster, null, null), duo),
				BattleEnemySelection(arrayListOf(monster, monster, monster, null), trio)
			),
			sharedEnemies = null,
			sharedLevelRange = levelRange,
			ownLevelRange = null,
			minSteps = 30,
			chance = 10,
			defaultBackground = parsed.randomBattles!!.defaultBackground,
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
		assertEquals(AreaSnowType.None, parsed.properties.snowType)
		assertEquals(2, parsed.objects.doors.size)
		assertEquals(1, parsed.objects.walkTriggers.size)

		assertEquals(10, parsed.objects.decorations.count { it.sprites?.flashName == "torch(2, 16)" })
		assertEquals(10, parsed.objects.decorations.count { it.sprites?.flashName == "SkullBrazier(0, 32)" })
	}

	@Test
	fun testParseGoldfishWarp() {
		val parsed = parseArea(content, "goldfish_warp", ArrayList(), ArrayList())
		assertEquals(AreaAmbience(rgba(16, 14, 10, 100), 0), parsed.properties.ambience)
	}

	@Test
	fun testParseAirTemple() {
		val parsed = parseArea(content, "aeropolis_N_TAIR", ArrayList(), ArrayList())
		assertEquals(1, parsed.objects.transitions.size)
		assertEquals(1, parsed.objects.objects.size)
		assertEquals(1, parsed.objects.characters.size)
		assertEquals("Aeropolis", parsed.properties.musicTrack)
		assertTrue(parsed.flags.hasClearMap)
		// TODO Add error support
	}

	@Test
	fun testParseTheatre() {
		val parsed = parseArea(content, "aeropolis_W_theatre", ArrayList(), ArrayList())
		assertEquals(20, parsed.objects.characters.size)
	}

	@Test
	fun testParseGuardPost() {
		val parsed = parseArea(content, "guardpost", ArrayList(), ArrayList())
		assertEquals(2, parsed.objects.characters.size)
		assertEquals(2, parsed.objects.transitions.size)
	}

	@Test
	fun testDesertCave() {
		val yinYang = content.items.items.find { it.flashName == "Yin and Yang" }!!
		val parsed = parseArea(content, "desertcave", ArrayList(), ArrayList())
		assertEquals(1, parsed.chests.size)

		val chest = parsed.chests[0]
		assertEquals(6, chest.x)
		assertEquals(3, chest.y)
		assertEquals(0, chest.gold)
		assertEquals(ItemStack(yinYang, 1), chest.stack)
		assertFalse(chest.hidden)

		val battle = chest.battle!!
		assertSame(content.battle.enemyPartyLayouts.find { it.name == "DRAGON" }, battle.enemyLayout)
		assertEquals("BossBattle", battle.specialMusic)

		val monsters = battle.monsters
		assertEquals(4, monsters.size)

		for (index in 1 until 4) assertNull(monsters[index])
		val demon = monsters[0]!!
		assertEquals("Bone Demon", demon.name1)
		assertEquals("Bone Demon", demon.name2)
		assertEquals(40, demon.level)
	}

	@Test
	fun testParseSunTemple1() {
		val parsed = parseArea(content, "sunTemple1", ArrayList(), ArrayList())
		assertEquals(1, parsed.chests.size)
		assertNull(parsed.randomBattles)

		val chest = parsed.chests[0]
		assertEquals(14, chest.x)
		assertEquals(1, chest.y)
		assertEquals(3000, chest.gold)
		assertNull(chest.stack)
		assertNull(chest.plotItem)
		assertNull(chest.dreamstone)
		assertNull(chest.battle)
		assertFalse(chest.hidden)
	}

	@Test
	fun testParseSunTemple4() {
		val spear = content.items.items.find { it.flashName == "Iron Spear" }!!
		val parsed = parseArea(content, "sunTemple4", ArrayList(), ArrayList())
		assertEquals(7, parsed.chests.size)

		val chest = parsed.chests[1]
		assertEquals(16, chest.x)
		assertEquals(51, chest.y)
		assertEquals(0, chest.gold)
		assertEquals(ItemStack(spear, 1), chest.stack)
		assertNull(chest.plotItem)
		assertNull(chest.dreamstone)
		assertFalse(chest.hidden)

		val battle = chest.battle!!
		assertNull(battle.monsters[3])
		for (monster in battle.monsters.sliceArray(0 until 3)) {
			assertEquals("BloodLizard", monster!!.name1)
			assertEquals("Blood Lizard", monster.name2)
			assertEquals(15, monster.level)
		}
		assertEquals("TRIO", battle.enemyLayout.name)
		assertNull(battle.specialMusic)
	}

	@Test
	fun testTaintedGrotto() {
		val parsed = parseArea(content, "pcave3", ArrayList(), ArrayList())

		val randomBattles = parsed.randomBattles!!
		assertSame(content.areas.enemySelections.find { it.name == "TAINTED_GROTTO" }!!, randomBattles.sharedEnemies!!)

		val chest = parsed.chests[0]
		assertEquals(12, chest.x)
		assertEquals(45, chest.y)
		assertNull(chest.stack)
		assertEquals(0, chest.gold)
		assertSame(content.items.plotItems.find { it.name == "Trilobite Key I" }!!, chest.plotItem)
		assertTrue(chest.hidden)
		assertNull(chest.dreamstone)
		assertNull(chest.battle)
	}

	@Test
	fun testDreamcave() {
		val parsed = parseArea(content, "canonia_dreamcave_d2", ArrayList(), ArrayList())
		assertEquals(3, parsed.chests.size)

		val dreamstone = parsed.chests[0]
		assertEquals(2, dreamstone.x)
		assertEquals(19, dreamstone.y)
		assertEquals(0, dreamstone.gold)
		assertNull(dreamstone.stack)
		assertNull(dreamstone.plotItem)
		assertSame(content.items.dreamstones.find { it.index == 11 }!!, dreamstone.dreamstone)
		assertNull(dreamstone.battle)
		assertFalse(dreamstone.hidden)

		val finger = parsed.chests[1]
		assertEquals(4, finger.x)
		assertEquals(19, finger.y)
		assertEquals(0, finger.gold)
		assertEquals(ItemStack(content.items.items.find { it.flashName == "Yggdrasil's Finger" }!!, 1), finger.stack)
		assertNull(finger.plotItem)
		assertNull(finger.dreamstone)
		assertNull(finger.battle)
		assertFalse(finger.hidden)

		val talisman = parsed.chests[2]
		assertEquals(7, talisman.x)
		assertEquals(2, talisman.y)
		assertEquals(0, talisman.gold)
		assertNull(talisman.stack)
		assertSame(content.items.plotItems.find { it.name == "Talisman of ONEIROS" }!!, talisman.plotItem)
		assertNull(talisman.dreamstone)
		assertNull(talisman.battle)
		assertFalse(talisman.hidden)
	}
}
