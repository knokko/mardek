package mardek.importer.battle

import mardek.assets.animations.BattleModel
import mardek.assets.battle.Monster
import mardek.assets.battle.PotentialEquipment
import mardek.assets.battle.PotentialItem
import mardek.importer.combat.importCombatAssets
import mardek.importer.inventory.importInventoryAssets
import mardek.importer.skills.importSkills
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

private const val FALLEN_PALADIAN_PROPERTIES = """
mdlStats = {names:["Fallen Paladin"],model:"FallenPaladin",Class:"Paladin",TYPE:"UNDEAD",cElem:"LIGHT",wpnType:"GREATAXE",armrTypes:[],baseStats:{hp:8744,mp:86,STR:28,VIT:22,SPR:10,AGL:8},nAtk:15,nDef:0,nMDef:0,critical:3,hpGrowth:0,atkGrowth:[0,0],equip:{weapon:["Rending Eye"],shield:["none"],helmet:["none"],armour:["Righteous Honour"],accs:["Gauntlet"],accs2:["YalortianAmulet"]},resist:{LIGHT:80,DARK:-100,FIRE:-50,EARTH:50,ETHER:-50,PSN:100,PAR:100,CRS:100,DRK:100,NUM:100,SIL:100,SLP:100,CNF:100,ZOM:100,BLD:100},EXP:3000};
Techs = [{skill:"Power Attack",type:"ACT",DMG:["m",1.5,10],MP:0,accuracy:66,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"powerhit",desc:"Attack fiercly. 50% more powerful than an average attack, but less accurate."},{skill:"Counterattack",type:"ACT",DMG:["m",1,0],MP:0,accuracy:100,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"dmgplus",sfx:"hit_MARTIAL",desc:"Normal attack."},{skill:"Cleave",type:"ACT",DMG:50,MP:0,accuracy:100,critical:0,AP:0,MODE:"P",elem:"NONE",TT:"ALL_p",pfx:["Bolt",16777198]},{skill:"RRAAGH!",type:"ACT",MP:0,accuracy:100,AP:20,stfx:{BSK:100,HST:100},MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"chakra"}];
Gambits = [{command:"Counterattack",target:"ANY_PC",criteria:["random",0]},{command:"RRAAGH!",target:"SELF",criteria:["HP<",0.25],uses:1},{command:"Attack",target:"ANY_PC",criteria:null}];
Counters = {M:[["Counterattack",100]],P:[["Counterattack",100]]};
loot = [["Rending Eye",100],["Zombie Dust",20],["Dead Hand",10],["HolyWater",10]];
DetermineStats();
"""

class TestMonsterImporter {

	private val combatAssets = importCombatAssets()
	private val skillAssets = importSkills(combatAssets)
	private val itemAssets = importInventoryAssets(combatAssets, skillAssets)

	private fun getItem(name: String) = itemAssets.items.find { it.flashName == name }!!

	private fun getStatValue(monster: Monster, name: String) = monster.baseStats[combatAssets.stats.find { it.flashName == name }!!] ?: 0

	private fun getElementalResistance(monster: Monster, element: String) = monster.elementalResistances.find { it.element.rawName == element }?.modifier ?: 0f

	private fun getStatusResistance(monster: Monster, rawName: String) = monster.statusResistances.find { it.effect.flashName == rawName }?.chance ?: 0

	@Test
	fun testParseForestFish() {
		val forestFish = importMonsterStats("Forest Fish", BattleModel(), FOREST_FISH_PROPERTIES, combatAssets, itemAssets)

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
		assertEquals(0, getStatValue(forestFish, "evasion"))
		assertEquals(3, forestFish.critChance)
		assertEquals(6, forestFish.hpPerLevel)
		assertEquals(3, forestFish.attackPerLevelNumerator)
		assertEquals(2, forestFish.attackPerLevelDenominator)
		assertEquals(100, forestFish.experience)
		assertEquals(2, forestFish.elementalResistances.size)
		assertEquals(0.4f, getElementalResistance(forestFish, "EARTH"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(forestFish, "AIR"), 0.01f)
		assertEquals(0, forestFish.statusResistances.size)
		assertEquals(0, forestFish.attackEffects.size)

		assertEquals(PotentialEquipment.EMPTY, forestFish.weapon)
		assertEquals(PotentialEquipment.EMPTY, forestFish.shield)
		assertEquals(PotentialEquipment.EMPTY, forestFish.helmet)
		assertEquals(PotentialEquipment.EMPTY, forestFish.armor)
		assertEquals(PotentialEquipment.EMPTY, forestFish.accessory1)
		assertEquals(PotentialEquipment.EMPTY, forestFish.accessory2)

		assertEquals(2, forestFish.loot.size)
		assertEquals(PotentialItem(getItem("Fish Scales"), 10), forestFish.loot[0])
		assertEquals(PotentialItem(getItem("Potion"), 10), forestFish.loot[1])

		assertEquals(0, forestFish.actions.size)
		// TODO Gambits
	}

	@Test
	fun testParseAbomination() {
		val abomination = importMonsterStats("Abomination", BattleModel(), ABOMINATION_PROPERTIES, combatAssets, itemAssets)

		assertEquals("Undead", abomination.className)
		assertSame(combatAssets.races.find { it.flashName == "UNDEAD" }!!, abomination.type)
		assertSame(combatAssets.elements.find { it.rawName == "DARK" }!!, abomination.element)
		assertEquals(50, getStatValue(abomination, "hp"))
		assertEquals(36, getStatValue(abomination, "mp"))
		assertEquals(13, getStatValue(abomination, "STR"))
		assertEquals(17, getStatValue(abomination, "VIT"))
		assertEquals(15, getStatValue(abomination, "SPR"))
		assertEquals(5, getStatValue(abomination, "AGL"))
		assertEquals(15, getStatValue(abomination, "ATK"))
		assertEquals(0, getStatValue(abomination, "DEF"))
		assertEquals(0, getStatValue(abomination, "MDEF"))
		assertEquals(0, getStatValue(abomination, "evasion"))
		assertEquals(3, abomination.critChance)
		assertEquals(18, abomination.hpPerLevel)
		assertEquals(0, abomination.attackPerLevelNumerator)
		assertEquals(0, abomination.attackPerLevelDenominator)
		assertEquals(160, abomination.experience)

		assertEquals(4, abomination.elementalResistances.size)
		assertEquals(-1f, getElementalResistance(abomination, "LIGHT"), 0.01f)
		assertEquals(2f, getElementalResistance(abomination, "DARK"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(abomination, "FIRE"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(abomination, "ETHER"), 0.01f)
		assertEquals(11, abomination.statusResistances.size)
		for (name in arrayOf("PSN", "PAR", "CRS", "DRK", "NUM", "SIL", "SLP", "CNF", "ZOM", "BLD", "BSK")) {
			assertEquals(100, getStatusResistance(abomination, name))
		}
		assertEquals(0, abomination.attackEffects.size)

		assertEquals(PotentialEquipment(arrayListOf(PotentialItem(getItem("Shadowblade"), 100))), abomination.weapon)
		assertEquals(PotentialEquipment(arrayListOf(
			PotentialItem(getItem("Bronze Shield"), 25),
			PotentialItem(null, 75)
		)), abomination.shield)
		assertEquals(PotentialEquipment.EMPTY, abomination.helmet)
		assertEquals(PotentialEquipment.EMPTY, abomination.armor)
		assertEquals(PotentialEquipment(arrayListOf(
			PotentialItem(getItem("OnyxRing"), 50),
			PotentialItem(null, 50)
		)), abomination.accessory1)
		assertEquals(PotentialEquipment(arrayListOf(
			PotentialItem(getItem("SilverRing"), 50),
			PotentialItem(null, 50)
		)), abomination.accessory2)

		assertEquals(4, abomination.loot.size)
		assertEquals(PotentialItem(getItem("Zombie Dust"), 20), abomination.loot[0])
		assertEquals(PotentialItem(getItem("Dead Hand"), 10), abomination.loot[1])
		assertEquals(PotentialItem(getItem("Shadowblade"), 5), abomination.loot[2])
		assertEquals(PotentialItem(getItem("OnyxRing"), 5), abomination.loot[3])

		assertEquals(1, abomination.actions.size)
		val curse = abomination.actions[0]
		assertEquals(1, curse.addStatusEffects.size)
		assertSame(combatAssets.statusEffects.find { it.flashName == "CRS" }!!, curse.addStatusEffects[0].effect)
		assertEquals(60, curse.addStatusEffects[0].chance)
		// TODO Potion actions
		// TODO Gambits
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
