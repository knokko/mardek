package mardek.importer.battle

import mardek.assets.animations.BattleModel
import mardek.assets.battle.Monster
import mardek.importer.combat.importCombatAssets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

private const val FOREST_FISH_PROPERTIES = """
mdlStats = {names:["Forest Fish"],model:"flyingfish",sprite:"forestfish",Class:"Flying Fish",TYPE:"ICHTHYD",cElem:"WATER",baseStats:{hp:6,mp:10,STR:12,VIT:12,SPR:11,AGL:10},nAtk:4,nDef:0,nMDef:0,critical:3,evasion:0,hpGrowth:6,atkGrowth:[3,2],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["none"],accs:["none"],accs2:["none"]},resist:{EARTH:40,AIR:-50,PSN:0,PAR:0,DRK:0,CNF:0,NUM:0,SIL:0,CRS:0,SLP:0,ZOM:0,BSK:0,BLD:0},EXP:100};
Techs = [];
Gambits = [{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["Fish Scales",10],["Potion",10]];
DetermineStats();
"""

private const val ABOMINATION_PROPERTIES = """
mdlStats = {names:["Abomination"],model:"zombie",sprite:"zombie",Class:"Undead",TYPE:"UNDEAD",cElem:"DARK",wpnType:"none",armrTypes:[],baseStats:{hp:50,mp:36,STR:13,VIT:17,SPR:15,AGL:5},nAtk:15,nDef:0,nMDef:0,critical:3,hpGrowth:18,atkGrowth:[0,0],equip:{weapon:["Shadowblade"],shield:["none","none","none","Bronze Shield"],helmet:["none"],armour:["none"],accs:["none","OnyxRing"],accs2:["none","SilverRing"]},resist:{LIGHT:-100,DARK:200,FIRE:-50,ETHER:-50,PSN:100,PAR:100,CRS:100,DRK:100,NUM:100,SIL:100,SLP:100,CNF:100,ZOM:100,BLD:100,BSK:100},EXP:160};
Techs = [{skill:"Curse",type:"ACT",MP:2,accuracy:100,critical:0,stfx:{CRS:60},AP:0,MODE:"M",elem:"DARK",TT:"SINGLE",pfx:"curse",desc:"Magic attack."},{skill:"Potion",MODE:"I",item:"Potion",ID:-1,uses:1}];
Gambits = [{command:"Curse",target:"ANY_PC",criteria:["random",25]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["Zombie Dust",20],["Dead Hand",10],["Shadowblade",5],["OnyxRing",5]];
DetermineStats();
"""

private const val GHOUL_PROPERTIES = """
mdlStats = {names:["Ghoul"],model:"ghoul",sprite:"ghoul",Class:"Undead",TYPE:"UNDEAD",cElem:"DARK",wpnType:"none",armrTypes:[],baseStats:{hp:84,mp:132,STR:20,VIT:20,SPR:5,AGL:19},nAtk:19,nDef:0,nMDef:0,critical:3,hpGrowth:19,atkGrowth:[0,0],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["Leather Armour"],accs:["none"],accs2:["none"]},stfx_onhit:{PAR:10},resist:{LIGHT:-100,DARK:200,FIRE:-50,ETHER:-50,PSN:100,PAR:100,CRS:100,DRK:100,NUM:100,SIL:100,SLP:100,CNF:100,ZOM:100,BLD:100,BSK:100},EXP:150};
Techs = [_root.GetMONSTER_SKILL("Morbid Fondle"),_root.GetMONSTER_SKILL("Blood Drain")];
Gambits = [{command:"Blood Drain",target:"ANY_PC",criteria:["random",30]},{command:"Morbid Fondle",target:"ANY_PC",criteria:["random",30]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["Zombie Dust",20],["Dead Hand",10],["MotionPotion",10]];
DetermineStats();
""" // TODO Note the stfx_onhit

class TestMonsterImporter {

	private val combatAssets = importCombatAssets()

	private fun getStatValue(monster: Monster, name: String) = monster.baseStats[combatAssets.stats.find { it.flashName == name }!!] ?: 0

	private fun getElementalResistance(monster: Monster, element: String) = monster.elementalResistances.find { it.element.properName == element }?.modifier ?: 0f

	@Test
	fun testParseForestFish() {
		val forestFish = importMonsterStats("Forest Fish", BattleModel(), FOREST_FISH_PROPERTIES)

		assertEquals("Flying Fish", forestFish.className)
		assertSame(combatAssets.races.find { it.flashName == "ICHTHYD" }!!, forestFish.type)
		assertSame(combatAssets.elements.find { it.rawName == "WATER" }!!, forestFish.element)
		assertEquals(6, getStatValue(forestFish, "hp"))
		assertEquals(10, getStatValue(forestFish, "mp"))
		assertEquals(12, getStatValue(forestFish, "STR"))
		assertEquals(12, getStatValue(forestFish, "VIT"))
		assertEquals(11, getStatValue(forestFish, "SPR"))
		assertEquals(10, getStatValue(forestFish, "AGL"))
		assertEquals(4, getStatValue(forestFish, "ATK"))
		assertEquals(0, getStatValue(forestFish, "DEF"))
		assertEquals(0, getStatValue(forestFish, "MDEF"))
		assertEquals(3, forestFish.critChance)
		assertEquals(0, forestFish.evasionChance)
		assertEquals(6, forestFish.hpPerLevel)
		assertEquals(3, forestFish.attackPerLevelNumerator)
		assertEquals(2, forestFish.attackPerLevelDenominator)
		assertEquals(100, forestFish.experience)
		assertEquals(2, forestFish.elementalResistances.size)
		assertEquals(0.4f, getElementalResistance(forestFish, "EARTH"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(forestFish, "AIR"), 0.01f)
		assertEquals(0, forestFish.statusResistances.size)
		// TODO Equipment and loot
	}

	@Test
	fun testParseAbomination() {

	}

	@Test
	fun testParseSkeleton() {
		val raw = OVERRIDE_SKELETON_STATS
	}

	@Test
	fun testParseGhoul() {

	}

	@Test
	fun testBernardChapter3() {

	}
}
