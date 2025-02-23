package mardek.importer.battle

import mardek.assets.animations.BattleModel
import mardek.assets.battle.*
import mardek.assets.skill.SkillTargetType
import mardek.importer.combat.importCombatAssets
import mardek.importer.inventory.importInventoryAssets
import mardek.importer.skills.importSkills
import org.junit.jupiter.api.Assertions.*
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
"""

private const val FALLEN_PALADIN_PROPERTIES = """
mdlStats = {names:["Fallen Paladin"],model:"FallenPaladin",Class:"Paladin",TYPE:"UNDEAD",cElem:"LIGHT",wpnType:"GREATAXE",armrTypes:[],baseStats:{hp:8744,mp:86,STR:28,VIT:22,SPR:10,AGL:8},nAtk:15,nDef:0,nMDef:0,critical:3,hpGrowth:0,atkGrowth:[0,0],equip:{weapon:["Rending Eye"],shield:["none"],helmet:["none"],armour:["Righteous Honour"],accs:["Gauntlet"],accs2:["YalortianAmulet"]},resist:{LIGHT:80,DARK:-100,FIRE:-50,EARTH:50,ETHER:-50,PSN:100,PAR:100,CRS:100,DRK:100,NUM:100,SIL:100,SLP:100,CNF:100,ZOM:100,BLD:100},EXP:3000};
Techs = [{skill:"Power Attack",type:"ACT",DMG:["m",1.5,10],MP:0,accuracy:66,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"powerhit",desc:"Attack fiercly. 50% more powerful than an average attack, but less accurate."},{skill:"Counterattack",type:"ACT",DMG:["m",1,0],MP:0,accuracy:100,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"dmgplus",sfx:"hit_MARTIAL",desc:"Normal attack."},{skill:"Cleave",type:"ACT",DMG:50,MP:0,accuracy:100,critical:0,AP:0,MODE:"P",elem:"NONE",TT:"ALL_p",pfx:["Bolt",16777198]},{skill:"RRAAGH!",type:"ACT",MP:0,accuracy:100,AP:20,stfx:{BSK:100,HST:100},MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"chakra"}];
Gambits = [{command:"Counterattack",target:"ANY_PC",criteria:["random",0]},{command:"RRAAGH!",target:"SELF",criteria:["HP<",0.25],uses:1},{command:"Attack",target:"ANY_PC",criteria:null}];
Counters = {M:[["Counterattack",100]],P:[["Counterattack",100]]};
loot = [["Rending Eye",100],["Zombie Dust",20],["Dead Hand",10],["HolyWater",10]];
DetermineStats();
"""

private const val MYSTERY_MAN_PROPERTIES = """
mdlStats = {names:["Mystery Man"],model:"steeleBoss1",unique_sprite:"steele",portrait:"steele",sprite2:"steele",Class:"Brawler",TYPE:"HUMAN",cElem:"DARK",wpnType:"none",armrTypes:[],baseStats:{hp:3845,mp:200,STR:20,VIT:18,SPR:5,AGL:13},nAtk:30,nDef:0,nMDef:0,critical:3,hpGrowth:0,atkGrowth:[0,0],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["Night Jacket"],accs:["RingOfSTR+2"],accs2:["Yellow Fairy"]},resist:{AIR:-120,ETHER:50,PSN:10,PAR:10,DRK:10,CNF:10,NUM:10,SIL:10,CRS:10,SLP:10,ZOM:90,BSK:3,BLD:1},initialSTFX:{HST:1},EXP:4000};
Techs = [{skill:"Counterattack",type:"ACT",DMG:30,MP:0,accuracy:100,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"dmgplus",sfx:"punch"},{skill:"Fierce Strike",type:"ACT",DMG:60,MP:0,accuracy:50,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"powerhit",sfx:"punch"},{skill:"Catastrophic Punch",type:"ACT",DMG:60,MP:0,accuracy:90,stat_mod:{VIT:2},critical:100,AP:20,MODE:"P",elem:"DARK",TT:"SINGLE",pfx:"catpunch"},{skill:"Shut The Hell Up!",type:"ACT",DMG:30,MP:0,stfx:{SIL:100},accuracy:95,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"mutepunch"},{skill:"Arm Breaker",type:"ACT",DMG:30,MP:0,stfx:{NUM:100},accuracy:95,AP:20,MODE:"P",elem:"FIRE",TT:"SINGLE",pfx:"numbpunch"},{skill:"Stunner!",type:"ACT",DMG:30,MP:0,stfx:{PAR:100},accuracy:95,AP:20,MODE:"P",elem:"AIR",TT:"SINGLE",pfx:"stunpunch",sfx:"punch"},{skill:"Chakra",type:"ACT",DMG:100,MP:20,accuracy:255,AP:20,MODE:"M",elem:"FIRE",TT:"SELF",pfx:"chakra",SPECIAL:{ignore_shield:true},special:{HEALING:1}},{skill:"Mirrilixir",MODE:"I",item:"Mirrilixir",ID:-1,uses:1},{skill:"Noxious Bomb",MODE:"I",item:"Noxious Bomb",ID:-1}];
Gambits = [{command:"Counterattack",target:"ANY_PC",criteria:["random",0]},{command:"Mirrilixir",target:"SELF",criteria:["random",100],uses:1},{command:"Chakra",target:"SELF",criteria:["HP<",0.2],random:60},{command:"Noxious Bomb",target:"ANY_PC",criteria:["random",10]},{command:"Catastrophic Punch",target:"ANY_PC",criteria:["random",25]},{command:"Stunner!",target:"ANY_PC",criteria:["random",10]},{command:"Shut The Hell Up!",target:"ANY_PC",criteria:["random",10]},{command:"Arm Breaker",target:"ANY_PC",criteria:["random",10]},{command:"Chakra",target:"SELF",criteria:["HP<",0.3]},{command:"Fierce Strike",target:"ANY_PC",criteria:["random",100]},{command:"Attack",target:"ANY_PC",criteria:null}];
Counters = {M:[["Shut The Hell Up!",25]],P:[["Arm Breaker",10],["Stunner!",10],["Fierce Strike",10],["Counterattack",100]]};
loot = [["RingOfSTR+2",100],["Mirrilixir",100]];
DetermineStats();
"""

private const val ANIMUS_PROPERTIES = """
mdlStats = {names:["K\'destralan Mind"],model:"Kdestralan_Mind",Class:"Spirit",TYPE:"SPIRIT",cElem:"ETHER",baseStats:{hp:30000,mp:999,STR:1,VIT:20,SPR:42,AGL:50},nAtk:0,nDef:30,nMDef:10,critical:3,hpGrowth:0,atkGrowth:[0,0],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["none"],accs:["none"],accs2:["none"]},initialSTFX:{RGN:1,HST:1},resist:{NONE:100,FIRE:50,WATER:50,AIR:50,EARTH:50,DARK:50,LIGHT:50,ETHER:200,FIG:-100,PSN:100,PAR:100,DRK:100,CNF:100,NUM:100,SIL:100,CRS:100,SLP:100,ZOM:100,BSK:100,BLD:100},EXP:25000};
Techs = [{skill:"Rainbow Guard",type:"ACT",buff:true,stfx:{FN1:100,WN1:100,AN1:100,EN1:100,LN1:100,DN1:100,SN1:100,MN1:100},MP:0,accuracy:100,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"null_tetra"},{skill:"Delay Buster",type:"ACT",DMG:200,MP:0,remove_stfx:{HST:100},stat_mod:{AGL:[1,10]},critical:0,accuracy:100,MODE:"M",elem:"AIR",TT:"ALL_p",pfx:"delaybuster",ALL_PFX:true},{skill:"Shield Breaker: Annihilation",type:"ACT",DMG:200,MP:0,remove_stfx:{PSH:100,MSH:100},stat_mod:{MDEF:[1,10]},critical:0,accuracy:100,MODE:"M",elem:"THAUMA",TT:"ALL_p",pfx:"annihilation",ALL_PFX:true},{skill:"Mega Barrier",type:"ACT",buff:true,stfx:{PSH:100,MSH:100},stat_mod:{SPR:-5,DEF:-5,MDEF:-5},MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"LIGHT",TT:"SELF",pfx:"shield",uses:1},{skill:"Soulstorm Alpha",type:"ACT",DMG:["SPECIAL","HP%",0.5],remove_stfx:{GST:50},no_dmg_split:true,MP:0,accuracy:100,MODE:"M",elem:"ETHER",TT:"ALL_p",pfx:"soulstorm",ALL_PFX:true},{skill:"Soulstorm Gamma",type:"ACT",DMG:["SPECIAL","HP%",0.9],remove_stfx:{GST:75},no_dmg_split:true,MP:0,accuracy:100,MODE:"M",elem:"ETHER",TT:"ALL_p",pfx:"soulstorm",ALL_PFX:true},{skill:"Soulstorm Omega",type:"ACT",DMG:["SPECIAL","HP%",1.2],remove_stfx:{GST:100},no_dmg_split:true,ignore_modifiers:true,MP:0,accuracy:100,MODE:"M",elem:"ETHER",TT:"ALL_p",pfx:"soulstorm",ALL_PFX:true},{skill:"Ion Storm",type:"ACT",DMG:100,MP:0,accuracy:100,stfx:{PAR:10},stat_mod:{SPR:[1,3],MDEF:[1,3]},MODE:"M",elem:"ETHER",TT:"ALL_p",pfx:"ionstorm",ALL_PFX:true},{skill:"Energy Vortex: Fire",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"energy_burst_FIRE"},{skill:"Energy Vortex: Water",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"WATER",TT:"SINGLE",pfx:"energy_burst_WATER"},{skill:"Energy Vortex: Air",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"AIR",TT:"SINGLE",pfx:"energy_burst_AIR"},{skill:"Energy Vortex: Earth",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"EARTH",TT:"SINGLE",pfx:"energy_burst_EARTH"},{skill:"Energy Vortex: Light",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"energy_burst_LIGHT"},{skill:"Energy Vortex: Dark",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"DARK",TT:"SINGLE",pfx:"energy_burst_DARK"},{skill:"Energy Vortex: Aether",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"ETHER",TT:"SINGLE",pfx:"energy_burst_ETHER"},{skill:"Energy Vortex: Fig",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"FIG",TT:"SINGLE",pfx:"energy_burst_FIG"},{skill:"Energy Vortex: Thauma",type:"ACT",DMG:60,MP:0,critical:0,accuracy:100,AP:0,MODE:"M",elem:"THAUMA",TT:"SINGLE",pfx:"energy_burst_THAUMA"}];
Gambits = [{command:"Mega Barrier",target:"SELF",criteria:["HP<",0.25],uses:1},{command:"Rainbow Guard",target:"SELF",criteria:["random",5]},{command:"Shield Breaker: Annihilation",target:"ALL_p",criteria:["has_status","PSH"],random:60,OtherTurn:1},{command:"Shield Breaker: Annihilation",target:"ALL_p",criteria:["has_status","MSH"],random:60,OtherTurn:1},{command:"Delay Buster",target:"ALL_p",criteria:["has_status","HST"],random:60,OtherTurn:1},{command:"Energy Vortex: Fire",target:"ANY_PC",criteria:["resist<","FIRE",50],random:12,OtherTurn:0},{command:"Energy Vortex: Water",target:"ANY_PC",criteria:["resist<","WATER",50],random:12,OtherTurn:0},{command:"Energy Vortex: Air",target:"ANY_PC",criteria:["resist<","AIR",50],random:12,OtherTurn:0},{command:"Energy Vortex: Earth",target:"ANY_PC",criteria:["resist<","EARTH",50],random:12,OtherTurn:0},{command:"Energy Vortex: Light",target:"ANY_PC",criteria:["resist<","LIGHT",50],random:12,OtherTurn:0},{command:"Energy Vortex: Dark",target:"ANY_PC",criteria:["resist<","DARK",50],random:12,OtherTurn:0},{command:"Energy Vortex: Aether",target:"ANY_PC",criteria:["resist<","ETHER",50],random:12,OtherTurn:0},{command:"Energy Vortex: Fig",target:"ANY_PC",criteria:["resist<","FIG",50],random:12,OtherTurn:0},{command:"Energy Vortex: Thauma",target:"ANY_PC",criteria:["resist<","THAUMA",50],random:12,OtherTurn:0},{command:"Ion Storm",target:"ALL_p",criteria:["random",30],OtherTurn:1},{command:"Soulstorm Omega",target:"ALL_p",criteria:["MYHP<",0.4]},{command:"Soulstorm Gamma",target:"ALL_p",criteria:["MYHP<",0.7]},{command:"Soulstorm Alpha",target:"ALL_p",criteria:["MYHP<",1]},{command:"Ion Storm",target:"ALL_p",criteria:null}];
Counters = {M:[],P:[]};
loot = [["Sorcerer\'s Soul",100],["Scarab of Protection",100],["Elixir",100],["PhoenixPinion",100]];
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

	private fun assertBasicAttack(strategy: StrategyEntry) {
		assertNull(strategy.skill)
		assertEquals(StrategyTarget.AnyPlayer, strategy.target)
	}

	private fun assertBasicAttackPool(pool: StrategyPool) {
		assertEquals(1, pool.entries.size)
		assertBasicAttack(pool.entries[0])
		assertEquals(100, pool.entries[0].chance)
		assertEquals(StrategyCriteria.NONE, pool.criteria)
	}

	private fun getLegionSkill(name: String) = skillAssets.classes.find { it.name == "Mimicry" }!!.actions.find { it.name == name }!!

	@Test
	fun testParseForestFish() {
		val forestFish = importMonsterStats(
			"Forest Fish", BattleModel(), FOREST_FISH_PROPERTIES, combatAssets, itemAssets, skillAssets
		)

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
		assertEquals(1, forestFish.strategies.size)
		assertBasicAttackPool(forestFish.strategies[0])

		assertEquals(0, forestFish.meleeCounterAttacks.size)
		assertEquals(0, forestFish.rangedCounterAttacks.size)
	}

	@Test
	fun testParseAbomination() {
		val abomination = importMonsterStats(
			"Abomination", BattleModel(), ABOMINATION_PROPERTIES, combatAssets, itemAssets, skillAssets
		)

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
		assertEquals(1, abomination.strategies.size)
		val pool = abomination.strategies[0]
		assertEquals(StrategyCriteria.NONE, pool.criteria)
		assertEquals(2, pool.entries.size)
		val curseStrategy = pool.entries[0]
		assertSame(curse, curseStrategy.skill)
		assertEquals(StrategyTarget.AnyPlayer, curseStrategy.target)
		assertEquals(25, curseStrategy.chance)
		val attackStrategy = pool.entries[1]
		assertBasicAttack(attackStrategy)
		assertEquals(75, attackStrategy.chance)

		assertEquals(0, abomination.meleeCounterAttacks.size)
		assertEquals(0, abomination.rangedCounterAttacks.size)
	}

	@Test
	fun testParseGhoul() {
		val ghoul = importMonsterStats(
			"Ghoul", BattleModel(), GHOUL_PROPERTIES, combatAssets, itemAssets, skillAssets
		)

		assertEquals("Undead", ghoul.className)
		assertSame(combatAssets.races.find { it.flashName == "UNDEAD" }!!, ghoul.type)
		assertSame(combatAssets.elements.find { it.rawName == "DARK" }!!, ghoul.element)
		assertEquals(84, getStatValue(ghoul, "hp"))
		assertEquals(132, getStatValue(ghoul, "mp"))
		assertEquals(20, getStatValue(ghoul, "STR"))
		assertEquals(20, getStatValue(ghoul, "VIT"))
		assertEquals(5, getStatValue(ghoul, "SPR"))
		assertEquals(19, getStatValue(ghoul, "AGL"))
		assertEquals(19, getStatValue(ghoul, "ATK"))
		assertEquals(0, getStatValue(ghoul, "DEF"))
		assertEquals(0, getStatValue(ghoul, "MDEF"))
		assertEquals(0, getStatValue(ghoul, "evasion"))
		assertEquals(3, ghoul.critChance)
		assertEquals(19, ghoul.hpPerLevel)
		assertEquals(0, ghoul.attackPerLevelNumerator)
		assertEquals(0, ghoul.attackPerLevelDenominator)
		assertEquals(150, ghoul.experience)

		assertEquals(4, ghoul.elementalResistances.size)
		assertEquals(-1f, getElementalResistance(ghoul, "LIGHT"), 0.01f)
		assertEquals(2f, getElementalResistance(ghoul, "DARK"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(ghoul, "FIRE"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(ghoul, "ETHER"), 0.01f)
		assertEquals(11, ghoul.statusResistances.size)
		for (name in arrayOf("PSN", "PAR", "CRS", "DRK", "NUM", "SIL", "SLP", "CNF", "ZOM", "BLD", "BSK")) {
			assertEquals(100, getStatusResistance(ghoul, name))
		}
		assertEquals(1, ghoul.attackEffects.size)
		assertEquals(10, ghoul.attackEffects[0].chance)
		assertSame(combatAssets.statusEffects.find { it.flashName == "PAR" }!!, ghoul.attackEffects[0].effect)

		assertEquals(PotentialEquipment.EMPTY, ghoul.weapon)
		assertEquals(PotentialEquipment.EMPTY, ghoul.shield)
		assertEquals(PotentialEquipment.EMPTY, ghoul.helmet)
		assertEquals(PotentialEquipment(arrayListOf(
			PotentialItem(getItem("Leather Armour"), 100),
		)), ghoul.armor)
		assertEquals(PotentialEquipment.EMPTY, ghoul.accessory1)
		assertEquals(PotentialEquipment.EMPTY, ghoul.accessory2)

		assertEquals(3, ghoul.loot.size)
		assertEquals(PotentialItem(getItem("Zombie Dust"), 20), ghoul.loot[0])
		assertEquals(PotentialItem(getItem("Dead Hand"), 10), ghoul.loot[1])
		assertEquals(PotentialItem(getItem("MotionPotion"), 10), ghoul.loot[2])

		assertEquals(0, ghoul.actions.size)

		assertEquals(1, ghoul.strategies.size)
		val pool = ghoul.strategies[0]
		assertEquals(StrategyCriteria.NONE, pool.criteria)
		assertEquals(3, pool.entries.size)
		val drainStrategy = pool.entries[0]
		assertSame(getLegionSkill("Blood Drain"), drainStrategy.skill)
		assertEquals(StrategyTarget.AnyPlayer, drainStrategy.target)
		assertEquals(30, drainStrategy.chance)
		val morbidStrategy = pool.entries[1]
		assertSame(getLegionSkill("Morbid Fondle"), morbidStrategy.skill)
		assertEquals(StrategyTarget.AnyPlayer, morbidStrategy.target)
		assertEquals(21, morbidStrategy.chance)
		val attackStrategy = pool.entries[2]
		assertBasicAttack(attackStrategy)
		assertEquals(49, attackStrategy.chance)

		assertEquals(0, ghoul.meleeCounterAttacks.size)
		assertEquals(0, ghoul.rangedCounterAttacks.size)
	}

	@Test
	fun testFallenPaladin() {
		val paladin = importMonsterStats(
			"Fallen Paladin", BattleModel(), FALLEN_PALADIN_PROPERTIES, combatAssets, itemAssets, skillAssets
		)
		assertEquals(2, paladin.actions.size)
		val counterAttack = paladin.actions[0]
		assertEquals("Counterattack", counterAttack.name)
		val counterDamage = counterAttack.damage!!
		assertEquals(1f, counterDamage.weaponModifier)
		assertEquals(100, counterAttack.accuracy)
		val boost = paladin.actions[1]
		assertEquals("RRAAGH!", boost.name)
		assertEquals(2, boost.addStatusEffects.size)
		assertEquals("Berserk", boost.addStatusEffects[0].effect.niceName)
		assertEquals("Haste", boost.addStatusEffects[1].effect.niceName)

		assertEquals(2, paladin.strategies.size)
		val boostPool = paladin.strategies[0]
		assertEquals(StrategyCriteria(maxUses = 1, hpPercentageAtMost = 25), boostPool.criteria)
		assertEquals(1, boostPool.entries.size)
		val boostStrategy = boostPool.entries[0]
		assertSame(boost, boostStrategy.skill)
		assertEquals(StrategyTarget.Self, boostStrategy.target)
		assertEquals(100, boostStrategy.chance)

		val attackPool = paladin.strategies[1]
		assertEquals(StrategyCriteria.NONE, attackPool.criteria)
		assertEquals(1, attackPool.entries.size)
		val attackStrategy = attackPool.entries[0]
		assertNull(attackStrategy.skill)
		assertEquals(StrategyTarget.AnyPlayer, attackStrategy.target)
		assertEquals(100, attackStrategy.chance)

		assertEquals(1, paladin.meleeCounterAttacks.size)
		assertSame(counterAttack, paladin.meleeCounterAttacks[0].action)
		assertEquals(100, paladin.meleeCounterAttacks[0].chance)
		assertEquals(StrategyTarget.AnyPlayer, paladin.meleeCounterAttacks[0].target)
		assertEquals(1, paladin.rangedCounterAttacks.size)
		assertSame(counterAttack, paladin.rangedCounterAttacks[0].action)
		assertEquals(100, paladin.rangedCounterAttacks[0].chance)
		assertEquals(StrategyTarget.AnyPlayer, paladin.rangedCounterAttacks[0].target)
	}

	@Test
	fun testBernardChapter3() {
		val bernard = importMonsterStats(
			"Bernard", BattleModel(), OVERRIDE_BERNARD_CHAPTER3, combatAssets, itemAssets, skillAssets
		)

		assertEquals(4, bernard.actions.size)
		val heh = bernard.actions[0]
		assertEquals("Heh.", heh.name)
		assertEquals("darkbolt", heh.particleEffect)
		assertEquals(100, heh.damage!!.flatAttackValue)
		assertTrue(heh.drainsBlood)
		val storm = bernard.actions[1]
		assertEquals("Thunderstorm", storm.name)
		assertEquals(SkillTargetType.AllAllies, storm.targetType)
		val immolate = bernard.actions[2]
		assertEquals("Immolate", immolate.name)
		assertEquals("FIRE", immolate.element.properName)
		assertEquals(50, immolate.damage!!.flatAttackValue)
		val glaciate = bernard.actions[3]
		assertEquals("Glaciate", glaciate.name)
		assertEquals("WATER", glaciate.element.properName)

		assertEquals(1, bernard.strategies.size)
		val pool = bernard.strategies[0]
		assertEquals(StrategyCriteria.NONE, pool.criteria)
		assertEquals(4, pool.entries.size)
		val stormStrategy = pool.entries[0]
		assertSame(storm, stormStrategy.skill)
		assertEquals(30, stormStrategy.chance)
		val immolateStrategy = pool.entries[1]
		assertSame(immolate, immolateStrategy.skill)
		assertEquals(35, immolateStrategy.chance)
		val glaciateStrategy = pool.entries[2]
		assertSame(glaciate, glaciateStrategy.skill)
		assertEquals(35, glaciateStrategy.chance)
		val hehStrategy = pool.entries[3]
		assertSame(heh, hehStrategy.skill)
		assertEquals(0, hehStrategy.chance) // Will only be used when Bernard runs out of mana

		assertEquals(0, bernard.rangedCounterAttacks.size)
		assertEquals(1, bernard.meleeCounterAttacks.size)
		assertSame(heh, bernard.meleeCounterAttacks[0].action)
		assertEquals(100, bernard.meleeCounterAttacks[0].chance)
		assertEquals(StrategyTarget.AnyPlayer, bernard.meleeCounterAttacks[0].target)
	}

	@Test
	fun testMysteryMan() {
		val mystery = importMonsterStats(
			"Mystery Man", BattleModel(), MYSTERY_MAN_PROPERTIES, combatAssets, itemAssets, skillAssets
		)

		assertEquals(7, mystery.actions.size)
		val counterAttack = mystery.actions[0]
		assertEquals("Counterattack", counterAttack.name)
		val fierceStrike = mystery.actions[1]
		assertEquals("Fierce Strike", fierceStrike.name)
		val catastrophicPunch = mystery.actions[2]
		assertEquals("Catastrophic Punch", catastrophicPunch.name)
		assertEquals(1, catastrophicPunch.statModifiers.size)
		assertEquals("VIT", catastrophicPunch.statModifiers[0].stat.flashName)
		assertEquals(-2, catastrophicPunch.statModifiers[0].minAdder)
		assertEquals(-2, catastrophicPunch.statModifiers[0].maxAdder)
		val shutUp = mystery.actions[3]
		assertEquals("Shut The Hell Up!", shutUp.name)
		val armBreaker = mystery.actions[4]
		assertEquals("Arm Breaker", armBreaker.name)
		val stunner = mystery.actions[5]
		assertEquals("Stunner!", stunner.name)
		val chakra = mystery.actions[6]
		assertEquals("Chakra", chakra.name)

		assertEquals(5, mystery.strategies.size)
		val mirrilixirPool = mystery.strategies[0]
		assertEquals(1, mirrilixirPool.entries.size)
		assertEquals(100, mirrilixirPool.entries[0].chance)
		assertSame(getItem("Mirrilixir"), mirrilixirPool.entries[0].item)
		assertEquals(StrategyTarget.Self, mirrilixirPool.entries[0].target)
		assertEquals(StrategyCriteria(maxUses = 1), mirrilixirPool.criteria)

		val chakraPool = mystery.strategies[1]
		assertEquals(1, chakraPool.entries.size)
		assertSame(chakra, chakraPool.entries[0].skill)
		assertEquals(60, chakraPool.entries[0].chance)
		assertEquals(StrategyTarget.Self, chakraPool.entries[0].target)
		assertEquals(StrategyCriteria(hpPercentageAtMost = 20), chakraPool.criteria)

		val normalPool = mystery.strategies[2]
		assertEquals(StrategyCriteria.NONE, normalPool.criteria)
		assertEquals(5, normalPool.entries.size)
		val noxiousEntry = normalPool.entries[0]
		assertSame(getItem("Noxious Bomb"), noxiousEntry.item)
		assertEquals(10, noxiousEntry.chance)
		assertEquals(StrategyTarget.AnyPlayer, noxiousEntry.target)
		val catastrophicEntry = normalPool.entries[1]
		assertSame(catastrophicPunch, catastrophicEntry.skill)
		assertEquals(23, catastrophicEntry.chance)
		assertEquals(StrategyTarget.AnyPlayer, catastrophicEntry.target)
		val stunnerEntry = normalPool.entries[2]
		assertSame(stunner, stunnerEntry.skill)
		assertEquals(7, stunnerEntry.chance)
		assertEquals(StrategyTarget.AnyPlayer, stunnerEntry.target)
		val shutUpEntry = normalPool.entries[3]
		assertSame(shutUp, shutUpEntry.skill)
		assertEquals(6, shutUpEntry.chance)
		assertEquals(StrategyTarget.AnyPlayer, shutUpEntry.target)
		val armBreakerEntry = normalPool.entries[4]
		assertSame(armBreaker, armBreakerEntry.skill)
		assertEquals(5, armBreakerEntry.chance)
		assertEquals(StrategyTarget.AnyPlayer, armBreakerEntry.target)

		val maybeChakraPool = mystery.strategies[3]
		assertEquals(StrategyCriteria(hpPercentageAtMost = 30), maybeChakraPool.criteria)
		assertEquals(1, maybeChakraPool.entries.size)
		assertSame(chakra, maybeChakraPool.entries[0].skill)
		assertEquals(100, maybeChakraPool.entries[0].chance)
		assertEquals(StrategyTarget.Self, maybeChakraPool.entries[0].target)

		val fiercePool = mystery.strategies[4]
		assertEquals(StrategyCriteria.NONE, fiercePool.criteria)
		assertEquals(2, fiercePool.entries.size)
		assertSame(fierceStrike, fiercePool.entries[0].skill)
		assertEquals(100, fiercePool.entries[0].chance)
		assertEquals(StrategyTarget.AnyPlayer, fiercePool.entries[0].target)
		assertNull(fiercePool.entries[1].skill)
		assertEquals(0, fiercePool.entries[1].chance)
		assertEquals(StrategyTarget.AnyPlayer, fiercePool.entries[1].target)

		assertEquals(1, mystery.rangedCounterAttacks.size)
		val shutUpCounter = mystery.rangedCounterAttacks[0]
		assertSame(shutUp, shutUpCounter.action)
		assertEquals(25, shutUpCounter.chance)
		assertEquals(StrategyTarget.AnyPlayer, shutUpCounter.target)

		assertEquals(4, mystery.meleeCounterAttacks.size)
		val armBreakCounter = mystery.meleeCounterAttacks[0]
		assertSame(armBreaker, armBreakCounter.action)
		assertEquals(10, armBreakCounter.chance)
		assertEquals(StrategyTarget.AnyPlayer, armBreakCounter.target)
		val stunnerCounter = mystery.meleeCounterAttacks[1]
		assertSame(stunner, stunnerCounter.action)
		assertEquals(9, stunnerCounter.chance)
		assertEquals(StrategyTarget.AnyPlayer, stunnerCounter.target)
		val fierceCounter = mystery.meleeCounterAttacks[2]
		assertSame(fierceStrike, fierceCounter.action)
		assertEquals(8, fierceCounter.chance)
		assertEquals(StrategyTarget.AnyPlayer, fierceCounter.target)
		val basicCounter = mystery.meleeCounterAttacks[3]
		assertSame(counterAttack, basicCounter.action)
		assertEquals(73, basicCounter.chance)
		assertEquals(StrategyTarget.AnyPlayer, basicCounter.target)
	}
	// TODO Test animus
}
