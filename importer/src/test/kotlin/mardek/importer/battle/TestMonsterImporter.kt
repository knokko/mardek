package mardek.importer.battle

import mardek.assets.animations.BattleModel
import mardek.assets.battle.*
import mardek.assets.combat.Element
import mardek.assets.skill.ActiveSkill
import mardek.assets.combat.ElementalResistance
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

private const val MASTER_STONE_PROPERTIES = """
mdlStats = {names:["Master Stone"],model:"gemstone",Class:"Gemstone",TYPE:"CRYSTAL",wpnType:"none",cElem:"ETHER",armrTypes:[],baseStats:{hp:5000,mp:600,STR:30,VIT:30,SPR:30,AGL:30},nAtk:30,nDef:20,nMDef:10,critical:3,hpGrowth:0,atkGrowth:[0,0],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["none"],accs:["none"],accs2:["none"]},initialSTFX:{HST:1},resist:{NONE:100,FIRE:100,AIR:100,EARTH:100,WATER:100,DARK:100,LIGHT:100,ETHER:100,FIG:100,PSN:100,PAR:100,CRS:100,DRK:100,NUM:100,SIL:100,SLP:100,CNF:100,ZOM:100,BLD:100,BSK:100},EXP:5000};
ShiftResistances = {FIRE:{NONE:100,FIRE:200,AIR:100,EARTH:100,WATER:-100,DARK:100,LIGHT:100,ETHER:100,FIG:100},WATER:{NONE:100,FIRE:100,AIR:100,EARTH:-100,WATER:200,DARK:100,LIGHT:100,ETHER:100,FIG:100},AIR:{NONE:100,FIRE:-100,AIR:200,EARTH:100,WATER:100,DARK:100,LIGHT:100,ETHER:100,FIG:100},EARTH:{NONE:100,FIRE:100,AIR:-100,EARTH:200,WATER:100,DARK:100,LIGHT:100,ETHER:100,FIG:100},LIGHT:{NONE:100,FIRE:100,AIR:100,EARTH:100,WATER:100,DARK:-100,LIGHT:200,ETHER:100,FIG:100},DARK:{NONE:100,FIRE:100,AIR:100,EARTH:100,WATER:100,DARK:200,LIGHT:-100,ETHER:100,FIG:100},ETHER:{NONE:100,FIRE:90,AIR:90,EARTH:90,WATER:90,DARK:80,LIGHT:80,ETHER:200,FIG:-100}};
Techs = [{skill:"Elemental Shift",type:"ACT",MP:0,MODE:"M",elem:"FIG",elementalShift:["FIRE","WATER","AIR","EARTH","LIGHT","DARK","ETHER"],TT:"SELF",pfx:"elementalshift"},{skill:"Gemsplosion: Fire Opal",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"FIRE",TT:"ALL_p",pfx:"gemsplosion_fire",desc:"Gem attack."},{skill:"Gemsplosion: Topaz",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"AIR",TT:"ALL_p",pfx:"gemsplosion_topaz",desc:"Gem attack."},{skill:"Gemsplosion: Onyx",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"DARK",TT:"ALL_p",pfx:"gemsplosion_onyx",desc:"Gem attack."},{skill:"Gemsplosion: Moonstone",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"LIGHT",TT:"ALL_p",pfx:"gemsplosion_moonstone",desc:"Gem attack."},{skill:"Gemsplosion: Emerald",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"EARTH",TT:"ALL_p",pfx:"gemsplosion_emerald",desc:"Gem attack."},{skill:"Gemsplosion: Aquamarine",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"WATER",TT:"ALL_p",pfx:"gemsplosion_turquoise",desc:"Gem attack."},{skill:"Gemsplosion: Turquoise",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"ETHER",TT:"ALL_p",pfx:"gemsplosion_turquoise",desc:"Gem attack."}];
Gambits = [{command:"Elemental Shift",target:"SELF",criteria:null,uses:1},{command:"Elemental Shift",target:"SELF",criteria:["NotLastTech"]},{command:"Gemsplosion: Fire Opal",target:"ALL_p",criteria:["elem=","FIRE"]},{command:"Gemsplosion: Topaz",target:"ALL_p",criteria:["elem=","AIR"]},{command:"Gemsplosion: Onyx",target:"ALL_p",criteria:["elem=","DARK"]},{command:"Gemsplosion: Moonstone",target:"ALL_p",criteria:["elem=","LIGHT"]},{command:"Gemsplosion: Emerald",target:"ALL_p",criteria:["elem=","EARTH"]},{command:"Gemsplosion: Aquamarine",target:"ALL_p",criteria:["elem=","WATER"]},{command:"Gemsplosion: Turquoise",target:"ALL_p",criteria:null},{command:"Attack",target:"ANY_PC"}];
loot = [["Candriathope",100]];
DetermineStats();
"""

class TestMonsterImporter {

	private val combatAssets = importCombatAssets()
	private val skillAssets = importSkills(combatAssets)
	private val itemAssets = importInventoryAssets(combatAssets, skillAssets)

	private fun getItem(name: String) = itemAssets.items.find { it.flashName == name }!!

	private fun getStatValue(monster: Monster, name: String) = monster.baseStats[combatAssets.stats.find { it.flashName == name }!!] ?: 0

	private fun getElementalResistance(monster: Monster, element: String) = monster.resistances.elements.find { it.element.rawName == element }?.modifier ?: 0f

	private fun getStatusResistance(monster: Monster, rawName: String) = monster.resistances.effects.find { it.effect.flashName == rawName }?.percentage ?: 0

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
		assertEquals(2, forestFish.resistances.elements.size)
		assertEquals(0.4f, getElementalResistance(forestFish, "EARTH"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(forestFish, "AIR"), 0.01f)
		assertEquals(0, forestFish.resistances.effects.size)
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

		assertEquals(4, abomination.resistances.elements.size)
		assertEquals(-1f, getElementalResistance(abomination, "LIGHT"), 0.01f)
		assertEquals(2f, getElementalResistance(abomination, "DARK"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(abomination, "FIRE"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(abomination, "ETHER"), 0.01f)
		assertEquals(11, abomination.resistances.effects.size)
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
		assertEquals(StrategyEntry(
			skill = curse, item = null, target = StrategyTarget.AnyPlayer, chance = 25
		), pool.entries[0])
		assertEquals(StrategyEntry(
			skill = null, item = null, target = StrategyTarget.AnyPlayer, chance = 75
		), pool.entries[1])

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

		assertEquals(4, ghoul.resistances.elements.size)
		assertEquals(-1f, getElementalResistance(ghoul, "LIGHT"), 0.01f)
		assertEquals(2f, getElementalResistance(ghoul, "DARK"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(ghoul, "FIRE"), 0.01f)
		assertEquals(-0.5f, getElementalResistance(ghoul, "ETHER"), 0.01f)
		assertEquals(11, ghoul.resistances.effects.size)
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
		assertEquals(StrategyEntry(
			skill = getLegionSkill("Blood Drain"), item = null, target = StrategyTarget.AnyPlayer, chance = 30
		), pool.entries[0])
		assertEquals(StrategyEntry(
			skill = getLegionSkill("Morbid Fondle"), item = null, target = StrategyTarget.AnyPlayer, chance = 21
		), pool.entries[1])
		assertEquals(StrategyEntry(
			skill = null, item = null, target = StrategyTarget.AnyPlayer, chance = 49
		), pool.entries[2])

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
		assertEquals(StrategyEntry(
			skill = boost, item = null, target = StrategyTarget.Self, chance = 100
		), boostPool.entries[0])

		val attackPool = paladin.strategies[1]
		assertEquals(StrategyCriteria.NONE, attackPool.criteria)
		assertEquals(1, attackPool.entries.size)
		assertEquals(StrategyEntry(
			skill = null, item = null, target = StrategyTarget.AnyPlayer, chance = 100
		), attackPool.entries[0])

		assertEquals(1, paladin.meleeCounterAttacks.size)
		assertEquals(CounterAttack(
			action = counterAttack, chance = 100, target = StrategyTarget.AnyPlayer
		), paladin.meleeCounterAttacks[0])
		assertEquals(paladin.meleeCounterAttacks, paladin.rangedCounterAttacks)
	}

	@Test
	fun testBernardChapter3() {
		val bernard = importMonsterStats(
			"Bernard", BattleModel(), OVERRIDE_BERNARD_CHAPTER3, combatAssets, itemAssets, skillAssets
		)
		assertEquals(4, bernard.playerStatModifier)

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
		assertEquals(StrategyEntry(
			skill = storm, item = null, target = StrategyTarget.AllPlayers, chance = 30
		), pool.entries[0])
		assertEquals(StrategyEntry(
			skill = immolate, item = null, target = StrategyTarget.AnyPlayer, chance = 35
		), pool.entries[1])
		assertEquals(StrategyEntry(
			skill = glaciate, item = null, target = StrategyTarget.AnyPlayer, chance = 35
		), pool.entries[2])
		assertEquals(StrategyEntry(
			skill = heh, item = null, target = StrategyTarget.AnyPlayer, chance = 0
		), pool.entries[3]) // Will only be used when Bernard runs out of mana

		assertEquals(0, bernard.rangedCounterAttacks.size)
		assertEquals(1, bernard.meleeCounterAttacks.size)
		assertEquals(CounterAttack(
			action = heh, chance = 100, target = StrategyTarget.AnyPlayer
		), bernard.meleeCounterAttacks[0])
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
		assertEquals(StrategyCriteria(maxUses = 1), mirrilixirPool.criteria)
		assertEquals(1, mirrilixirPool.entries.size)
		assertEquals(StrategyEntry(
			skill = null, item = getItem("Mirrilixir"), target = StrategyTarget.Self, chance = 100
		), mirrilixirPool.entries[0])

		val chakraPool = mystery.strategies[1]
		assertEquals(StrategyCriteria(hpPercentageAtMost = 20), chakraPool.criteria)
		assertEquals(1, chakraPool.entries.size)
		assertEquals(StrategyEntry(
			skill = chakra, item = null, target = StrategyTarget.Self, chance = 60
		), chakraPool.entries[0])

		val normalPool = mystery.strategies[2]
		assertEquals(StrategyCriteria.NONE, normalPool.criteria)
		assertEquals(5, normalPool.entries.size)
		assertEquals(StrategyEntry(
			skill = null, item = getItem("Noxious Bomb"), target = StrategyTarget.AnyPlayer, chance = 10
		), normalPool.entries[0])
		assertEquals(StrategyEntry(
			skill = catastrophicPunch, item = null, target = StrategyTarget.AnyPlayer, chance = 23
		), normalPool.entries[1])
		assertEquals(StrategyEntry(
			skill = stunner, item = null, target = StrategyTarget.AnyPlayer, chance = 7
		), normalPool.entries[2])
		assertEquals(StrategyEntry(
			skill = shutUp, item = null, target = StrategyTarget.AnyPlayer, chance = 6
		), normalPool.entries[3])
		assertEquals(StrategyEntry(
			skill = armBreaker, item = null, target = StrategyTarget.AnyPlayer, chance = 5
		), normalPool.entries[4])

		val maybeChakraPool = mystery.strategies[3]
		assertEquals(StrategyCriteria(hpPercentageAtMost = 30), maybeChakraPool.criteria)
		assertEquals(1, maybeChakraPool.entries.size)
		assertEquals(StrategyEntry(
			skill = chakra, item = null, target = StrategyTarget.Self, chance = 100
		), maybeChakraPool.entries[0])

		val fiercePool = mystery.strategies[4]
		assertEquals(StrategyCriteria.NONE, fiercePool.criteria)
		assertEquals(2, fiercePool.entries.size)
		assertEquals(StrategyEntry(
			skill = fierceStrike, item = null, target = StrategyTarget.AnyPlayer, chance = 100
		), fiercePool.entries[0])
		assertEquals(StrategyEntry(
			skill = null, item = null, target = StrategyTarget.AnyPlayer, chance = 0
		), fiercePool.entries[1])

		assertEquals(1, mystery.rangedCounterAttacks.size)
		assertEquals(CounterAttack(
			action = shutUp, chance = 25, target = StrategyTarget.AnyPlayer
		), mystery.rangedCounterAttacks[0])

		assertEquals(4, mystery.meleeCounterAttacks.size)
		assertEquals(CounterAttack(
			action = armBreaker, chance = 10, target = StrategyTarget.AnyPlayer
		), mystery.meleeCounterAttacks[0])
		assertEquals(CounterAttack(
			action = stunner, chance = 9, target = StrategyTarget.AnyPlayer
		), mystery.meleeCounterAttacks[1])
		assertEquals(CounterAttack(
			action = fierceStrike, chance = 8, target = StrategyTarget.AnyPlayer
		), mystery.meleeCounterAttacks[2])
		assertEquals(CounterAttack(
			action = counterAttack, chance = 73, target = StrategyTarget.AnyPlayer
		), mystery.meleeCounterAttacks[3])
	}

	@Test
	fun testAnimus() {
		val animus = importMonsterStats(
			"A. Animus", BattleModel(), ANIMUS_PROPERTIES, combatAssets, itemAssets, skillAssets
		)

		assertEquals(17, animus.actions.size)
		val rainbowGuard = animus.actions[0]
		assertEquals("Rainbow Guard", rainbowGuard.name)
		assertEquals(8, rainbowGuard.addStatusEffects.size)
		val delayBuster = animus.actions[1]
		assertEquals("Delay Buster", delayBuster.name)
		assertEquals(1, delayBuster.removeStatusEffects.size)
		val shieldBreak = animus.actions[2]
		assertEquals("Shield Breaker: Annihilation", shieldBreak.name)
		assertEquals(200, shieldBreak.damage!!.flatAttackValue)
		val megaBarrier = animus.actions[3]
		assertEquals("Mega Barrier", megaBarrier.name)
		assertEquals(2, megaBarrier.addStatusEffects.size)
		val alpha = animus.actions[4]
		assertEquals("Soulstorm Alpha", alpha.name)
		assertEquals(0.5f, alpha.damage!!.remainingTargetHpModifier)
		val gamma = animus.actions[5]
		assertEquals("Soulstorm Gamma", gamma.name)
		val omega = animus.actions[6]
		assertEquals("Soulstorm Omega", omega.name)
		val ionStorm = animus.actions[7]
		assertEquals("Ion Storm", ionStorm.name)
		assertEquals(1, ionStorm.addStatusEffects.size)

		val vortexMap = mutableMapOf<Element, ActiveSkill>()
		for (index in 8 until 17) {
			assertTrue(animus.actions[index].name.contains("Energy Vortex"))
			assertFalse(vortexMap.containsKey(animus.actions[index].element))
			vortexMap[animus.actions[index].element] = animus.actions[index]
		}

		assertEquals(18, animus.strategies.size)

		val barrierPool = animus.strategies[0]
		assertEquals(StrategyCriteria(maxUses = 1, hpPercentageAtMost = 25), barrierPool.criteria)
		assertEquals(1, barrierPool.entries.size)
		assertEquals(StrategyEntry(
			skill = megaBarrier, item = null, target = StrategyTarget.Self, chance = 100
		), barrierPool.entries[0])

		val guardPool = animus.strategies[1]
		assertEquals(StrategyCriteria.NONE, guardPool.criteria)
		assertEquals(1, guardPool.entries.size)
		assertEquals(StrategyEntry(
			skill = rainbowGuard, item = null, target = StrategyTarget.Self, chance = 5
		), guardPool.entries[0])

		val shieldPool1 = animus.strategies[2]
		assertEquals(StrategyCriteria(
			targetHasEffect = combatAssets.statusEffects.find { it.flashName == "PSH" }!!,
			canUseOnEvenTurns = false
		), shieldPool1.criteria)
		assertEquals(1, shieldPool1.entries.size)
		assertEquals(StrategyEntry(
			skill = shieldBreak, item = null, target = StrategyTarget.AllPlayers, chance = 60
		), shieldPool1.entries[0])

		val shieldPool2 = animus.strategies[3]
		assertEquals(StrategyCriteria(
			targetHasEffect = combatAssets.statusEffects.find { it.flashName == "MSH" }!!,
			canUseOnEvenTurns = false
		), shieldPool2.criteria)
		assertEquals(shieldPool1.entries, shieldPool2.entries)

		val delayPool = animus.strategies[4]
		assertEquals(StrategyCriteria(
			targetHasEffect = combatAssets.statusEffects.find { it.niceName == "Haste" }!!,
			canUseOnEvenTurns = false
		), delayPool.criteria)
		assertEquals(1, delayPool.entries.size)
		assertEquals(StrategyEntry(
			skill = delayBuster, item = null, target = StrategyTarget.AllPlayers, chance = 60
		), delayPool.entries[0])

		for (index in 5 until 14) {
			val vortexPool = animus.strategies[index]
			val element = vortexPool.entries[0].skill!!.element
			assertEquals(StrategyCriteria(
				resistanceAtMost = ElementalResistance(element, 0.5f),
				canUseOnOddTurns = false
			), vortexPool.criteria)
			assertEquals(1, vortexPool.entries.size)
			assertEquals(StrategyEntry(
				skill = vortexMap.remove(element)!!, item = null, target = StrategyTarget.AnyPlayer, chance = 12
			), vortexPool.entries[0])
		}

		val ionPool = animus.strategies[14]
		assertEquals(StrategyCriteria(canUseOnEvenTurns = false), ionPool.criteria)
		assertEquals(1, ionPool.entries.size)
		assertEquals(StrategyEntry(
			skill = ionStorm, item = null, target = StrategyTarget.AllPlayers, chance = 30
		), ionPool.entries[0])

		val omegaPool = animus.strategies[15]
		assertEquals(StrategyCriteria(hpPercentageAtMost = 40), omegaPool.criteria)
		assertEquals(1, omegaPool.entries.size)
		assertEquals(StrategyEntry(
			skill = omega, item = null, target = StrategyTarget.AllPlayers, chance = 100
		), omegaPool.entries[0])

		val gammaPool = animus.strategies[16]
		assertEquals(StrategyCriteria(hpPercentageAtMost = 70), gammaPool.criteria)
		assertEquals(1, gammaPool.entries.size)
		assertEquals(StrategyEntry(
			skill = gamma, item = null, target = StrategyTarget.AllPlayers, chance = 100
		), gammaPool.entries[0])

		val alphaPool = animus.strategies[17]
		assertEquals(StrategyCriteria.NONE, alphaPool.criteria)
		assertEquals(2, alphaPool.entries.size)
		assertEquals(StrategyEntry(
			skill = alpha, item = null, target = StrategyTarget.AllPlayers, chance = 100
		), alphaPool.entries[0])
		assertEquals(StrategyEntry(
			skill = ionStorm, item = null, target = StrategyTarget.AllPlayers, chance = 0
		), alphaPool.entries[1])

		assertEquals(0, animus.meleeCounterAttacks.size)
		assertEquals(0, animus.rangedCounterAttacks.size)
	}

	@Test
	fun testMasterStone() {
		val stone = importMonsterStats(
			"Master Stone", BattleModel(), MASTER_STONE_PROPERTIES, combatAssets, itemAssets, skillAssets
		)

		assertEquals(0, stone.playerStatModifier)
		assertEquals(arrayListOf(combatAssets.statusEffects.find { it.niceName == "Haste" }!!), stone.initialEffects)

		val fire = combatAssets.elements.find { it.rawName == "FIRE" }!!
		val fireResistances = stone.elementalShiftResistances[fire]!!
		for (name in arrayOf("NONE", "AIR", "EARTH", "DARK", "LIGHT", "ETHER", "FIG")) {
			assertEquals(1f, fireResistances.elements.find { it.element.rawName == name }!!.modifier)
		}
		assertEquals(2f, fireResistances.elements.find { it.element === fire }!!.modifier)
		assertEquals(-1f, fireResistances.elements.find { it.element.properName == "WATER" }!!.modifier)

		assertEquals(8, stone.actions.size)
		val elementalShift = stone.actions[0]
		assertEquals("Elemental Shift", elementalShift.name)
		assertEquals(SkillTargetType.Self, elementalShift.targetType)
		assertTrue(elementalShift.changeElement)
		val gemsplosionFire = stone.actions[1]
		assertEquals("Gemsplosion: Fire Opal", gemsplosionFire.name)
		assertSame(fire, gemsplosionFire.element)
		assertEquals(50, gemsplosionFire.damage!!.flatAttackValue)

		val shiftStrategy = stone.strategies[1]
		assertEquals(StrategyCriteria(canRepeat = false), shiftStrategy.criteria)
		assertEquals(1, shiftStrategy.entries.size)
		assertEquals(StrategyEntry(
			skill = elementalShift,
			item = null,
			target = StrategyTarget.Self,
			chance = 100
		), shiftStrategy.entries[0])

		val fireStrategy = stone.strategies[2]
		assertEquals(StrategyCriteria(myElement = fire), fireStrategy.criteria)
		assertEquals(1, fireStrategy.entries.size)
		assertEquals(StrategyEntry(
			skill = gemsplosionFire,
			item = null,
			target = StrategyTarget.AllPlayers,
			chance = 100
		), fireStrategy.entries[0])
	}
}
