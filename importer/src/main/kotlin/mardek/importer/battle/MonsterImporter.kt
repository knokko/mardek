package mardek.importer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.jpexs.decompiler.flash.SWF
import com.jpexs.decompiler.flash.helpers.CodeFormatting
import com.jpexs.decompiler.flash.helpers.StringBuilderTextWriter
import com.jpexs.decompiler.flash.tags.*
import com.jpexs.decompiler.flash.types.ColorTransform
import com.jpexs.decompiler.flash.types.MATRIX
import com.jpexs.decompiler.flash.types.RECT
import mardek.content.Content
import mardek.content.animations.*
import mardek.content.battle.*
import mardek.content.stats.*
import mardek.content.inventory.Dreamstone
import mardek.content.inventory.ItemsContent
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import mardek.content.sprite.BcSprite
import mardek.importer.area.FLASH
import mardek.importer.area.parseFlashString
import mardek.importer.skills.SkillParseException
import mardek.importer.skills.parseActiveSkills
import mardek.importer.util.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.Integer.parseInt
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val OVERRIDE_BARTHOLIO_CHAPTER2 = """
mdlStats = {names:["Bartholio"],model:"bartholio",unique_sprite:"bartholio",Class:"Fighter",TYPE:"HUMAN",cElem:"LIGHT",wpnType:"GREATSWORD",armrTypes:[],baseStats:{hp:45,mp:30,STR:20,VIT:18,SPR:8,AGL:11},FAIR_STATS:2,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:18,atkGrowth:[0,0],equip:{weapon:["MythrilGreatblade"],shield:["none"],helmet:["none"],armour:["Bronze Armour"],accs:["Gauntlet"],accs2:["GreenBeads"]},resist:{ZOM:100},EXP:1000};
Techs = [{skill:"Counterattack",type:"ACT",DMG:["m",1],MP:0,accuracy:100,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"dmgplus",sfx:"hit_2HSWORDS"},{skill:"Power Attack",type:"ACT",DMG:["m",1.5,10],MP:0,accuracy:46,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",sfx:"hit_2HSWORDS",pfx:"powerhit"},{skill:"Avengance!",type:"ACT",MP:20,accuracy:100,AP:20,stfx:{BSK:100},MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"chakra"}];
Gambits = [{command:"Counterattack",target:"ANY_PC",criteria:["random",0]},{command:"Avengance!",target:"SELF",criteria:["alone"],uses:1},{command:"Power Attack",target:"ANY_PC",criteria:["random",30]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["PhoenixDown",100]];
DetermineStats();
"""

const val OVERRIDE_BARTHOLIO_CHAPTER3 = """
mdlStats = {names:["Bartholio"],model:"bartholio",unique_sprite:"bartholio",Class:"Fighter",TYPE:"HUMAN",cElem:"LIGHT",wpnType:"GREATSWORD",armrTypes:[],baseStats:{hp:45,mp:30,STR:20,VIT:18,SPR:8,AGL:11},FAIR_STATS:4,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:18,atkGrowth:[0,0],equip:{weapon:["Starmetal Blade"],shield:["none"],helmet:["Horned Helm"],armour:["Iron Armour"],accs:["Gauntlet"],accs2:["Emerald Bangle"]},resist:{ZOM:100},EXP:1000};
Techs = [{skill:"Counterattack",type:"ACT",DMG:["m",1],MP:0,accuracy:100,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",pfx:"dmgplus",sfx:"hit_2HSWORDS"},{skill:"Power Attack",type:"ACT",DMG:["m",1.5,10],MP:0,accuracy:72,AP:20,MODE:"P",elem:"NONE",TT:"SINGLE",sfx:"hit_2HSWORDS",pfx:"powerhit"},{skill:"Avengance!",type:"ACT",MP:20,accuracy:100,AP:20,stfx:{BSK:100},MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"chakra"}];
Gambits = [{command:"Counterattack",target:"ANY_PC",criteria:["random",0]},{command:"Avengance!",target:"SELF",criteria:["alone"],uses:1},{command:"Power Attack",target:"ANY_PC",criteria:["random",30]},{command:"Attack",target:"ANY_PC",criteria:null}];
Counters = {M:null,P:[["Counterattack",100]]};
loot = [["Elixir",100]];
DetermineStats();
"""

const val OVERRIDE_VENNIE_CHAPTER2 = """
mdlStats = {names:["Vennie"],model:"vennie",unique_sprite:"vennie",Class:"Thief",TYPE:"HUMAN",cElem:"AIR",wpnType:"DAGGER",armrTypes:[],baseStats:{hp:35,mp:50,STR:12,VIT:14,SPR:10,AGL:20},FAIR_STATS:2,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:14,atkGrowth:[0,0],equip:{weapon:["Dagger"],shield:["none"],helmet:["none"],armour:["Bandit Leather"],accs:["RingOfAGL"],accs2:["AirPendant"]},resist:{ZOM:100},EXP:1000};
Techs = [{skill:"Mass Haste",type:"ACT",MP:6,accuracy:100,stfx:{HST:100},lasts:30,AP:0,MODE:"M",elem:"AIR",TT:"ALL_p",pfx:"haste",desc:"Inflicts haste."},{skill:"Potion",MODE:"I",item:"Potion",ID:-1,uses:3},{skill:"MugwortJuice",MODE:"I",item:"MugwortJuice",ID:-1,uses:3},{skill:"Eye Gouge",type:"ACT",DMG:["m",1.3,5],MP:2,accuracy:90,stfx:{DRK:80},AP:0,MODE:"P",elem:"DARK",TT:"SINGLE",pfx:"sting_drk",desc:"Inflicts darkness."},{skill:"Viperfang",type:"ACT",DMG:["m",1.3,5],MP:2,accuracy:90,stfx:{PSN:80},AP:0,MODE:"P",elem:"EARTH",TT:"SINGLE",pfx:"sting_psn",desc:"Inflicts darkness."},{skill:"Slumberstab",type:"ACT",DMG:["m",1.3,5],MP:2,accuracy:90,stfx:{SLP:80},AP:0,MODE:"P",elem:"AIR",TT:"SINGLE",pfx:"sting_slp",desc:"Inflicts darkness."}];
Gambits = [{command:"Potion",target:"ANY_ALLY",criteria:["HP<",0.2],uses:3},{command:"Eye Gouge",target:"ANY_PC",criteria:["random",40]},{command:"Viperfang",target:"ANY_PC",criteria:["random",40]},{command:"Slumberstab",target:"ANY_PC",criteria:["random",70]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["Remedy",100]];
DetermineStats();
"""

const val OVERRIDE_VENNIE_CHAPTER3 = """
mdlStats = {names:["Vennie"],model:"vennie",unique_sprite:"vennie",Class:"Thief",TYPE:"HUMAN",cElem:"AIR",wpnType:"DAGGER",armrTypes:[],baseStats:{hp:35,mp:50,STR:12,VIT:14,SPR:10,AGL:20},FAIR_STATS:4,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:14,atkGrowth:[0,0],equip:{weapon:["Curved Dagger"],shield:["Rune Shield"],helmet:["Black Hat"],armour:["Bandit Leather"],accs:["Emerald Bangle"],accs2:["Snakestone"]},resist:{ZOM:100},EXP:1000};
Techs = [{skill:"Mass Haste",type:"ACT",MP:6,accuracy:100,stfx:{HST:100},lasts:30,AP:0,MODE:"M",elem:"AIR",TT:"ALL_p",pfx:"haste",desc:"Inflicts haste."},{skill:"Potion",MODE:"I",item:"Potion",ID:-1,uses:3},{skill:"MugwortJuice",MODE:"I",item:"MugwortJuice",ID:-1,uses:3},{skill:"Eye Gouge",type:"ACT",DMG:["m",1.3,5],MP:2,accuracy:90,stfx:{DRK:80},AP:0,MODE:"P",elem:"DARK",TT:"SINGLE",pfx:"sting_drk",desc:"Inflicts darkness."},{skill:"Viperfang",type:"ACT",DMG:["m",1.3,5],MP:2,accuracy:90,stfx:{PSN:80},AP:0,MODE:"P",elem:"EARTH",TT:"SINGLE",pfx:"sting_psn",desc:"Inflicts darkness."},{skill:"Slumberstab",type:"ACT",DMG:["m",1.3,5],MP:2,accuracy:90,stfx:{SLP:80},AP:0,MODE:"P",elem:"AIR",TT:"SINGLE",pfx:"sting_slp",desc:"Inflicts darkness."}];
Gambits = [{command:"MugwortJuice",target:"ANY_ALLY",criteria:["HP<",0.2],uses:3},{command:"Eye Gouge",target:"ANY_PC",criteria:["random",40]},{command:"Viperfang",target:"ANY_PC",criteria:["random",40]},{command:"Slumberstab",target:"ANY_PC",criteria:["random",70]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["Speedy Juice",100]];
DetermineStats();
"""

const val OVERRIDE_AALIA_CHAPTER2 = """
mdlStats = {names:["Aalia"],model:"aalia",unique_sprite:"aalia",Class:"Healer",TYPE:"HUMAN",cElem:"LIGHT",wpnType:"STAFF",armrTypes:[],baseStats:{hp:25,mp:140,STR:9,VIT:10,SPR:19,AGL:16},FAIR_STATS:2,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:10,atkGrowth:[0,0],equip:{weapon:["HeartStaff"],shield:["none"],helmet:["none"],armour:["Mage Robe"],accs:["SilverRing"],accs2:["WaterPendant"]},resist:{CNF:50,SIL:50,CRS:100,ZOM:100,BSK:100},EXP:1000};
Techs = [{skill:"Regen",type:"ACT",MP:6,AP:20,stfx:{RGN:100},lasts:10,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"regen",desc:""},{skill:"M. Shield",type:"ACT",MP:6,AP:20,stfx:{MSH:100},lasts:10,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"mshield",desc:""},{skill:"Shield",type:"ACT",MP:6,AP:20,stfx:{PSH:100},lasts:10,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"shield",desc:""},{skill:"Cure",type:"ACT",DMG:30,MP:4,AP:20,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"cure1",desc:"10+d6/L",special:{HEALING:1},menuse:1},{skill:"Cura",type:"ACT",DMG:100,MP:4,AP:20,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"cure2",desc:"10+d6/L",special:{HEALING:1},menuse:1}];
Gambits = [{command:"Cure",target:"ANY_ALLY",criteria:["HP<",0.4]},{command:"Shield",target:"ANY_ALLY",criteria:["no_status","PSH"]},{command:"M. Shield",target:"ANY_ALLY",criteria:["no_status","MSH"]},{command:"Regen",target:"ANY_ALLY",criteria:["no_status","RGN"]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["MugwortJuice",100]];
DetermineStats();
"""

const val OVERRIDE_AALIA_CHAPTER3 = """
mdlStats = {names:["Aalia"],model:"aalia",unique_sprite:"aalia",Class:"Healer",TYPE:"HUMAN",cElem:"LIGHT",wpnType:"STAFF",armrTypes:[],baseStats:{hp:25,mp:140,STR:9,VIT:10,SPR:19,AGL:16},FAIR_STATS:4,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:10,atkGrowth:[0,0],equip:{weapon:["Fire Staff"],shield:["none"],helmet:["Regal Crown"],armour:["Dark Robe"],accs:["GoldRing"],accs2:["Mind Crystal"]},resist:{CNF:50,SIL:50,CRS:100,ZOM:100,BSK:100},EXP:1000};
Techs = [{skill:"Regen",type:"ACT",MP:6,AP:20,stfx:{RGN:100},lasts:10,MODE:"M",elem:"LIGHT",TT:"ALL_e",pfx:"regen",desc:""},{skill:"M. Shield",type:"ACT",MP:6,AP:20,stfx:{MSH:100},lasts:10,MODE:"M",elem:"LIGHT",TT:"ALL_e",pfx:"mshield",desc:""},{skill:"Shield",type:"ACT",MP:6,AP:20,stfx:{PSH:100},lasts:10,MODE:"M",elem:"LIGHT",TT:"ALL_e",pfx:"shield",desc:""},{skill:"Cure",type:"ACT",DMG:30,MP:4,AP:20,MODE:"M",elem:"LIGHT",TT:"SINGLE",pfx:"cure1",desc:"10+d6/L",special:{HEALING:1},menuse:1},{skill:"Cura",type:"ACT",DMG:100,MP:4,AP:20,MODE:"M",elem:"LIGHT",TT:"ALL_e",pfx:"cure2",desc:"10+d6/L",special:{HEALING:1},menuse:1}];
Gambits = [{command:"Cura",target:"ALL_e",criteria:["HP<",0.6]},{command:"Shield",target:"ALL_e",criteria:["no_status","PSH"]},{command:"M. Shield",target:"ALL_e",criteria:["no_status","MSH"]},{command:"Regen",target:"ALL_e",criteria:["no_status","RGN"]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["GoldRing",100]];
DetermineStats();
"""

const val OVERRIDE_BERNARD_CHAPTER2 = """
mdlStats = {names:["Bernard"],model:"bernard",unique_sprite:"bernard",Class:"Warlock",TYPE:"HUMAN",cElem:"DARK",wpnType:"WALKINGSTICK",armrTypes:[],baseStats:{hp:20,mp:240,STR:12,VIT:8,SPR:26,AGL:13},FAIR_STATS:2,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:8,atkGrowth:[0,0],equip:{weapon:["WalkingStick"],shield:["none"],helmet:["none"],armour:["Hempen Robe"],accs:["SilverRing"],accs2:["FirePendant"]},resist:{CNF:100,SIL:100,CRS:100,ZOM:100,BSK:100},EXP:1000};
Techs = [{skill:"Heh.",type:"ACT",DMG:100,MP:0,critical:0,accuracy:100,stfx:{CRS:100},AP:0,MODE:"M",elem:"DARK",TT:"SINGLE",pfx:"darkbolt",special:{DRAIN:1},desc:"Standard elemental magic attack."},{skill:"Thunderstorm",type:"ACT",DMG:16,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"AIR",TT:"ALL_p",pfx:"thunderstorm",ALL_PFX:true,CENTRED:true,ARENA:false,desc:"Standard elemental magic attack."},{skill:"Immolate",type:"ACT",DMG:16,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"pyromagia",desc:"Standard elemental magic attack."},{skill:"Glaciate",type:"ACT",DMG:16,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"WATER",TT:"SINGLE",pfx:"frostasia",desc:"Standard elemental magic attack."}];
Gambits = [{command:"Heh.",target:"ANY_PC",criteria:["random",0]},{command:"Thunderstorm",target:"ALL_p",criteria:["random",30]},{command:"Immolate",target:"ANY_PC",criteria:["random",50]},{command:"Glaciate",target:"ANY_PC",criteria:["random",100]},{command:"Heh.",target:"ANY_PC",criteria:null}];
loot = [["SilverRing",30]];
DetermineStats();
"""

const val OVERRIDE_BERNARD_CHAPTER3 = """
mdlStats = {names:["Bernard"],model:"bernard",unique_sprite:"bernard",Class:"Warlock",TYPE:"HUMAN",cElem:"DARK",wpnType:"WALKINGSTICK",armrTypes:[],baseStats:{hp:20,mp:240,STR:12,VIT:8,SPR:26,AGL:13},FAIR_STATS:4,nAtk:5,nDef:0,nMDef:0,critical:3,hpGrowth:8,atkGrowth:[0,0],equip:{weapon:["WalkingStick"],shield:["none"],helmet:["Black Hat"],armour:["Dark Robe"],accs:["Emerald Bangle"],accs2:["Sapphire Bangle"]},resist:{CNF:100,SIL:100,CRS:100,ZOM:100,BSK:100},EXP:1000};
Techs = [{skill:"Heh.",type:"ACT",DMG:100,MP:0,critical:0,accuracy:100,stfx:{CRS:100},AP:0,MODE:"M",elem:"DARK",TT:"SINGLE",pfx:"darkbolt",special:{DRAIN:1},desc:"Standard elemental magic attack."},{skill:"Thunderstorm",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"AIR",TT:"ALL_p",pfx:"thunderstorm",ALL_PFX:true,CENTRED:true,ARENA:false,desc:"Standard elemental magic attack."},{skill:"Immolate",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"pyromagia",desc:"Standard elemental magic attack."},{skill:"Glaciate",type:"ACT",DMG:50,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"WATER",TT:"SINGLE",pfx:"frostasia",desc:"Standard elemental magic attack."}];
Gambits = [{command:"Heh.",target:"ANY_PC",criteria:["random",0]},{command:"Thunderstorm",target:"ALL_p",criteria:["random",30]},{command:"Immolate",target:"ANY_PC",criteria:["random",50]},{command:"Glaciate",target:"ANY_PC",criteria:["random",100]},{command:"Heh.",target:"ANY_PC",criteria:null}];
Counters = {M:null,P:[["Heh.",100]]};
loot = [["Ether of Kings",100]];
DetermineStats();
"""

const val OVERRIDE_TEMPERANCE = """
mdlStats = {names:["Temperance"],model:"temperance",sprite:"temperance",Class:"Totem",TYPE:"CONSTRUCT",wpnType:"none",cElem:"FIRE",armrTypes:[],baseStats:{hp:800,mp:400,STR:20,VIT:10,SPR:10,AGL:10},nAtk:30,nDef:0,nMDef:0,critical:3,hpGrowth:0,atkGrowth:[0,0],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["none"],accs:["none"],accs2:["none"]},resist:{FIRE:100,AIR:50,WATER:-100,PSN:100,PAR:100,CRS:100,DRK:100,NUM:100,SIL:100,SLP:100,CNF:100,ZOM:100,BSK:100,BLD:100},EXP:400};
Techs = [_root.GetMONSTER_SKILL("Rage Chord"),{skill:"Flame",type:"ACT",DMG:26,MP:6,critical:0,accuracy:100,AP:0,MODE:"M",elem:"FIRE",TT:"SINGLE",pfx:"flame1",desc:"Standard elemental magic attack."}];
Gambits = [{command:"Rage Chord",target:"ANY_PC",criteria:["random",10]},{command:"Flame",target:"ANY_PC",criteria:["random",30]},{command:"Attack",target:"ANY_PC"}];
loot = [["BetterPotion",20]];
DetermineStats();
"""

const val OVERRIDE_MOLESTOR = """
mdlStats = {names:["Molestor"],model:"Molestor",sprite:"Molestor",Class:"Demon",TYPE:"DEMON",cElem:"DARK",wpnType:"none",armrTypes:[],baseStats:{hp:3333,mp:80,STR:25,VIT:22,SPR:25,AGL:15},nAtk:30,nDef:10,nMDef:10,critical:3,hpGrowth:0,atkGrowth:[0,0],equip:{weapon:["none"],shield:["none"],helmet:["none"],armour:["none"],accs:["none"],accs2:["none"]},resist:{DARK:200,LIGHT:-100,ETHER:-50,FIG:-100,PSN:0,PAR:0,DRK:100,CNF:0,NUM:0,SIL:0,CRS:100,SLP:0,ZOM:100,BSK:10,BLD:0},EXP:1333};
Techs = [{skill:"Darkside",type:"ACT",DMG:50,MP:16,critical:0,accuracy:100,stfx:{DRK:5,CRS:5},AP:0,MODE:"M",elem:"DARK",TT:"ALL",pfx:"darkside",ALL_PFX:true,desc:"Standard elemental magic attack."},{skill:"Curse",type:"ACT",MP:2,accuracy:100,critical:0,stfx:{CRS:100},AP:0,MODE:"M",elem:"DARK",TT:"ALL_p",pfx:"curse",desc:"Magic attack."},{skill:"Dark Claw",type:"ACT",DMG:["m",1.5],MP:10,critical:10,accuracy:100,stfx:{DRK:10},AP:0,MODE:"P",elem:"DARK",TT:"SINGLE",pfx:"darkclaw",desc:"Dark attack. More powerful than normal attack."}];
Gambits = [{command:"Darkside",target:"ALL_p",criteria:["random",50]},{command:"Dark Claw",target:"ANY_PC",criteria:["random",30]},{command:"Curse",target:"ANY_PC",criteria:["random",30]},{command:"Attack",target:"ANY_PC",criteria:null}];
loot = [["Dark Essence",100]];
DetermineStats();
"""

private fun importSkeleton(swf: SWF, id: Int, spriteIdMapping: MutableMap<Int, BcSprite>): Skeleton {
	val monsterTag = swf.tags.find { it.uniqueId == id.toString() }!! as DefineSpriteTag
	val monster = parseCreature2(monsterTag)
	return convertFlashCreature(monster, spriteIdMapping)
}

internal fun importMonsters(content: Content, playerModelMapping: MutableMap<String, BattleModel>) {
	val battleTag = FLASH.tags.find { it.uniqueId == "5118" }!! as DefineSpriteTag

	val labelBlacklist = arrayOf("null", "humans!", "dummy", "Monsters", "Bosses", "Ether Clone")
	val idBlacklist = arrayOf(-1, 0, 1885, 2233, 2294, 2311)

	class Monster(val label: String) {
		val placements = HashSet<Int>()
		val skins = HashSet<String>()
		val intSkins = HashSet<Int>()
		val exportedScripts = HashSet<String>()

		var finished = false

		override fun toString() = label
	}

	val monsters = mutableListOf<Monster>()
	var lastDoAction = ""

	fun extractSkin(monster: Monster, getSource: (StringBuilderTextWriter) -> Unit) {
		val builder = StringBuilder()
		val writer = StringBuilderTextWriter(CodeFormatting(), builder)
		getSource(writer)

		val source = builder.toString()
		if (source.contains("aeropolis_W_theatre")) {
			monster.skins.add("d")
			return
		}

		val prefix = "skin = "
		val index = source.indexOf(prefix)
		if (index == -1) {

			throw RuntimeException("monster is ${monster.label} and source is $source")
		}
		val endIndex = source.indexOf(';', index + prefix.length)
		if (endIndex == -1) throw RuntimeException("source is $source")

		val skinString = source.substring(index + prefix.length, endIndex)
		if (skinString.startsWith("\"")) monster.skins.add(skinString.substring(1, skinString.length - 1))
		else monster.intSkins.add(parseInt(skinString))
	}

	for (tag in battleTag.tags) {
		val monster = monsters.lastOrNull()
		if (monster != null && !monster.finished) {
			if (tag is PlaceObject2Tag && !idBlacklist.contains(tag.characterId)) {
				monster.placements.add(tag.characterId)
				for (child in tag.subItems) {
					extractSkin(monster) { child.getActionScriptSource(it, null) }
				}
			}
			if (tag is PlaceObject3Tag && !idBlacklist.contains(tag.characterId)) {
				monster.placements.add(tag.characterId)
				for (child in tag.subItems) {
					extractSkin(monster) { child.getActionScriptSource(it, null) }
				}
			}
		}
		if (tag is DoActionTag) {
			val builder = StringBuilder()
			val writer = StringBuilderTextWriter(CodeFormatting(), builder)
			tag.getActionScriptSource(writer, null)
			lastDoAction = builder.toString()
		}
		if (tag is FrameLabelTag) {
			val newMonster = Monster(tag.labelName)
			monsters.add(newMonster)
			if (lastDoAction.isNotEmpty()) newMonster.exportedScripts.add(lastDoAction)
			lastDoAction = ""
		}
		if (tag is ShowFrameTag && monster != null) monster.finished = true
	}

	monsters.removeIf { labelBlacklist.contains(it.label) }

	val skeletons = mutableMapOf<Int, Skeleton>()
	val spriteIdMapping = mutableMapOf<Int, BcSprite>()

	for ((index, monster) in monsters.withIndex()) {
		if (monster.placements.isEmpty() && index > 0) monster.placements.addAll(monsters[index - 1].placements)
		if (monster.placements.size != 1) throw RuntimeException("Monster ${monster.label} has ${monster.placements.size}")
		if (monster.exportedScripts.size > 1) {
			throw RuntimeException("Monster ${monster.label} has multiple scripts")
		}

		val skeleton = skeletons.computeIfAbsent(monster.placements.iterator().next()) { id -> importSkeleton(FLASH, id, spriteIdMapping) }

		if (monster.skins.size + monster.intSkins.size > 1) {
			throw RuntimeException("Monster ${monster.label} has multiple skins")
		}

		val skin = if (monster.skins.isNotEmpty()) monster.skins.iterator().next()
				else if (monster.intSkins.isNotEmpty()) "int-${monster.intSkins.iterator().next()}" else null

		if (monster.exportedScripts.isEmpty()) {
			playerModelMapping[monster.label] = BattleModel(skeleton, skin)
		} else {
			fun addMonster(name: String, propertiesText: String) {
				content.battle.monsters.add(importMonsterStats(
					name = name, model = BattleModel(skeleton, skin), propertiesText = propertiesText, content
				))
			}

			var propertiesText = monster.exportedScripts.iterator().next()
			if (monster.label == "Temperance") propertiesText = OVERRIDE_TEMPERANCE
			if (monster.label == "Molestor") propertiesText = OVERRIDE_MOLESTOR

			if (monster.label == "bartholio_e") {
				addMonster("BartholioChapter2", OVERRIDE_BARTHOLIO_CHAPTER2)
				addMonster("BartholioChapter3", OVERRIDE_BARTHOLIO_CHAPTER3)
				continue
			}
			if (monster.label == "vennie_e") {
				addMonster("VennieChapter2", OVERRIDE_VENNIE_CHAPTER2)
				addMonster("VennieChapter3", OVERRIDE_VENNIE_CHAPTER3)
				continue
			}
			if (monster.label == "aalia_e") {
				addMonster("AaliaChapter2", OVERRIDE_AALIA_CHAPTER2)
				addMonster("AaliaChapter3", OVERRIDE_AALIA_CHAPTER3)
				continue
			}
			if (monster.label == "bernard_e") {
				addMonster("BernardChapter2", OVERRIDE_BERNARD_CHAPTER2)
				addMonster("BernardChapter3", OVERRIDE_BERNARD_CHAPTER3)
				continue
			}
			addMonster(monster.label, propertiesText)
		}
	}

	for (skeleton in skeletons.values) content.battle.skeletons.add(skeleton)
}

private fun convertFlashCreature(creature: BattleCreature2, spriteIdMapping: MutableMap<Int, BcSprite>): Skeleton {
	val partMapping = mutableMapOf<Int, SkeletonPart>()
	val exportScale = 4
	val compressedShapesDirectory = File("$resourcesFolder/bc7shapes-x$exportScale")

	val parts = creature.bodyParts.map { rawBodyPart ->
		val skins = rawBodyPart.variations.map { BodyPart(name = it.name, entries = it.entries.map { rawEntry ->
			var sprite = spriteIdMapping[rawEntry.id]
			val bc7File = File("$compressedShapesDirectory/${rawEntry.id}.bc7")
			if (sprite == null) {
				if (bc7File.exists()) {
					val input = DataInputStream(Files.newInputStream(bc7File.toPath()))
					val width = input.readInt()
					val height = input.readInt()

					sprite = BcSprite(width, height, 7)
					sprite.data = input.readAllBytes()
					input.close()
				} else {
					val image = ImageIO.read(File("flash/monster-shapes-x$exportScale/${rawEntry.id}.png"))

					sprite = BcSprite(image.width, image.height, 7)
					sprite.bufferedImage = image
					sprite.postEncodeCallback = {
						compressedShapesDirectory.mkdirs()
						val output = DataOutputStream(Files.newOutputStream(bc7File.toPath()))
						output.writeInt(sprite.width)
						output.writeInt(sprite.height)
						output.write(sprite.data!!)
						output.flush()
						output.close()
					}
				}
				spriteIdMapping[rawEntry.id] = sprite
			}

			BodyPartEntry(sprite = sprite, offsetX = rawEntry.rect.Xmin / 20f, offsetY = rawEntry.rect.Ymin / 20f, scale = exportScale)
		}.toTypedArray()) }.toTypedArray()
		val skeletonPart = SkeletonPart(skins = skins)
		partMapping[rawBodyPart.id] = skeletonPart
		skeletonPart
	}
	val animations = creature.animations.mapValues { rawAnimation -> Animation(frames = rawAnimation.value.map { frame ->
		AnimationFrame(parts = frame.parts.filter { part -> part.matrix != null }.map { part ->
			val matrix = part.matrix!!
			val rawColor = part.color

			fun transform(value: Int) = (255.0 * (value / 256.0)).roundToInt()
			fun pack(r: Int, g: Int, b: Int, a: Int) = rgba(transform(r), transform(g), transform(b), transform(a))

			val color = if (rawColor != null) ColorTransform(
				addColor = pack(rawColor.redAdd, rawColor.greenAdd, rawColor.blueAdd, rawColor.alphaAdd),
				multiplyColor = pack(rawColor.redMulti, rawColor.greenMulti, rawColor.blueMulti, rawColor.alphaMulti)
			) else null
			AnimationPart(
				part = partMapping[part.part.id]!!,
				matrix = AnimationMatrix(
					translateX = matrix.translateX / 20f,
					translateY = matrix.translateY / 20f,
					rotateSkew0 = matrix.rotateSkew0,
					rotateSkew1 = matrix.rotateSkew1,
					hasScale = matrix.hasScale,
					scaleX = matrix.scaleX,
					scaleY = matrix.scaleY
				),
				color = color
			)
		}.toTypedArray())
	}.toTypedArray()) }
	return Skeleton(
		animations = HashMap(animations),
		parts = parts.toTypedArray(),
		strikePoint = creature.strikePoint,
		hitPoint = creature.hitPoint,
		statusPoint = creature.statusPoint,
		groundDistance = creature.groundDistance,
	)
}

private class FlashShapeEntry(val rect: RECT, val id: Int)

private class FlashShapeVariation(val name: String, val entries: List<FlashShapeEntry>)

private class BodyPart2(val id: Int, val variations: List<FlashShapeVariation>)

private class BattleCreature2(
	val bodyParts: Set<BodyPart2>, val minDepth: Int, val maxDepth: Int,
	val hitPoint: AnimationPoint, val strikePoint: AnimationPoint,
	val statusPoint: AnimationPoint, val groundDistance: Float,
) {
	lateinit var baseState: AnimationState

	val animations = mutableMapOf<String, List<AnimationState>>()
}

private class AnimationPartState {
	lateinit var part: BodyPart2
	var matrix: MATRIX? = null
	var color: ColorTransform? = null

	override fun toString() = "AnimationPS($matrix)"
}

private class AnimationState(private val creature: BattleCreature2) {
	val parts = Array(1 + creature.maxDepth - creature.minDepth) { AnimationPartState() }

	fun update(tag: Tag): Boolean {
		if (tag is PlaceObject2Tag && tag.depth - creature.minDepth < parts.size) {
			val part = parts[tag.depth - creature.minDepth]
			if (tag.characterId != 0 && tag.characterId != 2234 && tag.characterId != 2306 && tag.characterId != 4747) {
				part.part = creature.bodyParts.find { it.id == tag.characterId }!!
			}
			part.matrix = tag.matrix
			if (tag.placeFlagHasColorTransform) part.color = tag.colorTransform
			return true
		} else if (tag is RemoveObject2Tag) {
			val index = tag.depth - creature.minDepth
			if (index >= 0 && index < parts.size) {
				parts[index].matrix = null
				parts[index].color = null
			}
			return true
		}

		return false
	}

	fun copy(): AnimationState {
		val copied = AnimationState(creature)
		for ((index, part) in parts.withIndex()) {
			if (part.matrix != null) {
				copied.parts[index].part = part.part
				copied.parts[index].matrix = part.matrix
				copied.parts[index].color = part.color
			}
		}
		return copied
	}

	override fun toString() = parts.contentToString()
}

private fun parsePartSprites(partTag: DefineSpriteTag): List<FlashShapeVariation> {
	val shapes = mutableListOf<FlashShapeVariation>()

	var index = 0
	var frameLabel: FrameLabelTag? = null
	val singleShapes = mutableListOf<FlashShapeEntry>()
	while (index < partTag.tags.size()) {
		if (partTag.tags[index] is FrameLabelTag) frameLabel = partTag.tags[index] as FrameLabelTag?
		if (partTag.tags[index] is PlaceObject2Tag) {
			val placement = partTag.tags[index] as PlaceObject2Tag
			parseShape(partTag.swf, placement.characterId, singleShapes)
		}
		if (partTag.tags[index] is ShowFrameTag) {
			if (singleShapes.isNotEmpty()) {
				shapes.add(FlashShapeVariation(frameLabel?.labelName ?: "unknown", singleShapes.toList()))
				frameLabel = null
				singleShapes.clear()
			}
		}
		index += 1
	}

	if (shapes.isEmpty() && partTag.spriteId != 291) println("nothing for $partTag")
	return shapes
}

private fun parseShape(swf: SWF, id: Int, outShapes: MutableList<FlashShapeEntry>) {
	if (id == 0) return
	val shape = swf.tags.find { it.uniqueId == id.toString() } ?: throw RuntimeException("Can't find shape with ID $id")
	val rect = if (shape is DefineShape2Tag) shape.rect else if (shape is DefineShapeTag) shape.rect
	else if (shape is DefineShape3Tag) shape.rect else if (shape is DefineShape4Tag) shape.rect else null
	if (rect != null) {
		outShapes.add(FlashShapeEntry(rect, id))
	} else {
		if (id == 2281) return // TODO Handle glyph rune shield
		if (id == 1717) return // TODO Handle drill-o-matic
		println("unexpected shape $shape")
	}
}

private fun parseVariations(tag: Tag): List<FlashShapeVariation> {
	return if (tag is DefineSpriteTag) {
		if (tag.spriteId == 494) return emptyList() // TODO Support castSparkle
		parsePartSprites(tag)
	} else if (tag is DefineShapeTag || tag is DefineShape3Tag) {
		val singleShapes = mutableListOf<FlashShapeEntry>()
		parseShape(tag.swf, parseInt(tag.uniqueId), singleShapes)
		listOf(FlashShapeVariation("D", singleShapes))
	} else if (tag is DefineMorphShapeTag || tag is DefineMorphShape2Tag) {
		// TODO Do something?
		emptyList()
	} else {
		println("no DefineSpriteTag? ${tag::class.java} $tag")
		emptyList()
	}
}

private fun parseCreature2(creatureTag: DefineSpriteTag): BattleCreature2 {
	var minDepth = 1000
	var maxDepth = 0
	val bodyParts = mutableSetOf<BodyPart2>()
	var hitPoint: AnimationPoint? = null
	var strikePoint: AnimationPoint? = null
	var statusPoint: AnimationPoint? = null
	var groundDistance = 0f
	for (child in creatureTag.tags) {
		if (child is PlaceObject2Tag) {
			if (child.characterId == 2306) {
				val point = AnimationPoint(child.matrix.translateX * 0.05f, child.matrix.translateY * 0.05f)
				if (child.name == "HitPoint") hitPoint = point
				if (child.name == "StrikePoint") strikePoint = point
				if (child.name == "StfxPoint") statusPoint = point
			} else if (child.characterId == 2234 || child.characterId == 4747) {
				groundDistance = child.matrix.translateY * 0.05f
			} else {
				minDepth = min(minDepth, child.depth)
				maxDepth = max(maxDepth, child.depth)
				if (child.characterId != 0) bodyParts.add(BodyPart2(child.characterId, parseVariations(
					creatureTag.swf.tags.find { it.uniqueId == child.characterId.toString() }!!
				)))
			}
		}
	}

	if (statusPoint == null) statusPoint = hitPoint
	if (hitPoint == null || strikePoint == null) {
		throw RuntimeException("Tag $creatureTag misses HitPoint or StrikePoint")
	}
	val creature = BattleCreature2(
		bodyParts, minDepth, maxDepth, hitPoint = hitPoint,
		strikePoint = strikePoint, statusPoint = statusPoint!!,
		groundDistance = groundDistance
	)

	val animationState = AnimationState(creature)
	for (child in creatureTag.tags) {
		if (child is FrameLabelTag) break
		if (child is PlaceObject2Tag) {
			if (child.characterId == 2234 || child.characterId == 2306 || child.characterId == 4747) continue
		}
		animationState.update(child)
	}

	creature.baseState = animationState.copy()

	var currentLabel: String? = null
	val currentFrames = mutableListOf<AnimationState>()
	var hasChangedAnimation = false
	for (child in creatureTag.tags) {
		if (child is FrameLabelTag) {
			if (currentLabel != null) creature.animations[currentLabel] = currentFrames.toList()
			currentFrames.clear()
			currentLabel = child.labelName
		}

		if (child is ShowFrameTag && hasChangedAnimation) {
			hasChangedAnimation = false
			currentFrames.add(animationState.copy())
		}

		if (currentLabel != null) {
			hasChangedAnimation = animationState.update(child) || hasChangedAnimation
		}
	}

	return creature
}

private fun parsePotentialEquipment(equipmentText: String?, itemAssets: ItemsContent): PotentialEquipment {
	if (equipmentText == null) return PotentialEquipment.EMPTY
	val rawEquipmentList = parseActionScriptNestedList(equipmentText)
	if (rawEquipmentList !is ArrayList<*>) throw IllegalArgumentException("Unexpected equipment $equipmentText")

	val itemCounter = HashMap<Item?, Int>()
	for (rawEntry in rawEquipmentList) {
		val itemName = parseFlashString(rawEntry.toString(), "monster equipment entry")!!
		val item = if (itemName == "none") null else itemAssets.items.find { it.flashName == itemName }!!
		itemCounter[item] = (itemCounter[item] ?: 0) + 1
	}

	val chances = IntArray(itemCounter.size)
	val total = itemCounter.values.sum()
	for ((index, value) in itemCounter.values.withIndex()) chances[index] = 100 * value / total

	val newTotal = chances.sum()
	chances[chances.size - 1] += 100 - newTotal

	return PotentialEquipment(ArrayList(itemCounter.entries.mapIndexed { index, entry ->
		PotentialItem(entry.key, chances[index])
	}))
}

internal fun importMonsterStats(name: String, model: BattleModel, propertiesText: String, content: Content): Monster {
	val mimicry = content.skills.classes.find { it.name == "Mimicry" }!!
	val propertiesCode = parseActionScriptCode(listOf(propertiesText))
	val mdlMap = parseActionScriptObject(propertiesCode.variableAssignments["mdlStats"]!!)
	val typeName = parseFlashString(mdlMap["TYPE"]!!, "monster type")!!
	val elementName = parseFlashString(mdlMap["cElem"]!!, "monster element")!!

	val baseStats = HashMap<CombatStat, Int>()
	val rawBaseStats = parseActionScriptObject(mdlMap["baseStats"]!!)
	for ((statName, statValue) in rawBaseStats) {
		val stat = CombatStat.entries.find { it.flashName == statName }!!
		baseStats[stat] = parseInt(statValue)
	}

	var playerStatModifier = 0
	var rawFairStats = mdlMap["FAIR_STATS"]
	if (rawFairStats == "true") rawFairStats = "1"
	if (rawFairStats != null) playerStatModifier = parseInt(rawFairStats)

	val rawAttack = mdlMap["nAtk"]
	val rawMeleeDef = mdlMap["nDef"]
	val rawMagicDef = mdlMap["nMDef"]
	val rawEvasion = mdlMap["evasion"]
	for ((statName, statValue) in arrayOf(
		Pair("ATK", rawAttack), Pair("DEF", rawMeleeDef), Pair("MDEF", rawMagicDef), Pair("evasion", rawEvasion)
	)) {
		if (statValue == null) continue
		val stat = CombatStat.entries.find { it.flashName == statName }!!
		baseStats[stat] = parseInt(statValue)
	}

	var attackPerLevelNumerator = 0
	var attackPerLevelDenominator = 0
	val rawAttackGrowth = mdlMap["atkGrowth"]
	if (rawAttackGrowth != null) {
		val attackList = parseActionScriptNestedList(rawAttackGrowth)
		if (attackList !is ArrayList<*> || attackList.size != 2) {
			throw IllegalArgumentException("Unexpected attack growth $rawAttackGrowth")
		}
		attackPerLevelNumerator = parseInt(attackList[0].toString())
		attackPerLevelDenominator = parseInt(attackList[1].toString())
	}

	val rawLootList = parseActionScriptNestedList(propertiesCode.variableAssignments["loot"]!!)
	if (rawLootList !is ArrayList<*>) throw IllegalArgumentException("Unexpected loot $rawLootList")
	val loot = ArrayList<PotentialItem>(rawLootList.size)
	val plotLoot = ArrayList<PotentialPlotItem>(1)
	val dreamLoot = ArrayList<Dreamstone>(1)
	if (propertiesCode.variableAssignments["loot"]!!.contains(" ? ")) {
		println("Skipping annoying raw loot $rawLootList")
	} else {
		for (lootPair in rawLootList) {
			if (lootPair !is ArrayList<*> || lootPair.size < 2 || lootPair.size > 3) {
				throw IllegalArgumentException("Unexpected loot $rawLootList")
			}
			val chance = parseInt(lootPair[1].toString())
			val itemName = parseFlashString(lootPair[0].toString(), "loot item")!!
			if (itemName == "Dreamstone") {
				val index = parseInt(lootPair[2].toString())
				dreamLoot.add(content.items.dreamstones.find { it.index == index }!!)
			} else {
				val item = content.items.items.find { it.flashName == itemName }
				if (item != null) {
					val amount = if (lootPair.size == 2) 1 else parseInt(lootPair[2].toString())
					repeat(amount) { loot.add(PotentialItem(item, chance)) }
				} else {
					val plotItem = content.items.plotItems.find { it.name == itemName }!!
					plotLoot.add(PotentialPlotItem(plotItem, chance))
				}
			}
		}
	}

	val rawEquipmentMap = parseActionScriptObject(mdlMap["equip"]!!)

	val rawResistanceMap = parseActionScriptObject(mdlMap["resist"]!!)
	val elementalResistances = ArrayList<ElementalResistance>()
	val statusResistances = ArrayList<EffectResistance>()
	for ((source, rawResistance) in rawResistanceMap) {
		if (rawResistance == "0") continue
		val element = content.stats.elements.find { it.rawName == source }
		if (element != null) elementalResistances.add(ElementalResistance(element, parseInt(rawResistance) / 100f))
		else statusResistances.add(EffectResistance(content.stats.statusEffects.find { it.flashName == source }!!, parseInt(rawResistance)))
	}

	val attackEffects = ArrayList<PossibleStatusEffect>()
	val rawAttackEffects = mdlMap["stfx_onhit"]
	if (rawAttackEffects != null) {
		for ((effectName, chance) in parseActionScriptObject(rawAttackEffects)) {
			val effect = content.stats.statusEffects.find { it.flashName == effectName }!!
			attackEffects.add(PossibleStatusEffect(effect, parseInt(chance)))
		}
	}

	val initialEffects = ArrayList<StatusEffect>()
	val rawInitialEffects = mdlMap["initialSTFX"]
	if (rawInitialEffects != null) {
		val initialMap = parseActionScriptObject(rawInitialEffects)
		for ((effectName, one) in initialMap) {
			if (one != "1") throw SkillParseException("Unexpected initial effects $rawInitialEffects")
			initialEffects.add(content.stats.statusEffects.find { it.flashName == effectName }!!)
		}
	}

	val rawTechList = propertiesCode.variableAssignments["Techs"]!!.replace(
		Regex("_root.GetMONSTER_SKILL\\(\"[\\w, ]*\"\\)"), "{legion:true}"
	)
	val rawTechMap = parseActionScriptObjectList(rawTechList)

	val rawActionList = ArrayList<Map<String, String>>(rawTechMap.size)
	for (rawTechObject in rawTechMap) {
		if (rawTechObject.containsKey("legion") || rawTechObject["MODE"] == "\"I\"") continue
		rawActionList.add(rawTechObject)
	}

	val actions = parseActiveSkills(content, rawActionList, true)
	val targetMap = mutableMapOf<ActiveSkill, StrategyTarget>()
	val strategies = importMonsterStrategies(
		propertiesCode.variableAssignments["Gambits"]!!, actions, content, targetMap
	)
	var meleeCounterAttacks = ArrayList<CounterAttack>(0)
	var rangedCounterAttacks = ArrayList<CounterAttack>(0)
	val rawCounterAttacks = propertiesCode.variableAssignments["Counters"]
	if (rawCounterAttacks != null) {
		val counterMap = parseActionScriptObject(rawCounterAttacks)
		val rawMeleeCounters = counterMap["P"]
		if (rawMeleeCounters != null) meleeCounterAttacks = importCounterAttacks(
			rawMeleeCounters, actions, targetMap, mimicry.actions
		)
		val rawRangedCounters = counterMap["M"]
		if (rawRangedCounters != null) rangedCounterAttacks = importCounterAttacks(
			rawRangedCounters, actions, targetMap, mimicry.actions
		)
	}
	val usedActions = actions.filter {
		candidate -> strategies.any { pool -> pool.entries.any { it.skill === candidate } } ||
			meleeCounterAttacks.any { it.action === candidate } ||
			rangedCounterAttacks.any { it.action === candidate }
	}

	val rawShiftResistances = propertiesCode.variableAssignments["ShiftResistances"]
	val shiftResistances = HashMap<Element, Resistances>()
	if (rawShiftResistances != null) {
		val myMap = parseActionScriptObject(rawShiftResistances)
		for ((myRawElement, rawResistances) in myMap) {
			val myElement = content.stats.elements.find { it.rawName == myRawElement }!!
			shiftResistances[myElement] = Resistances()
			val resistanceMap = parseActionScriptObject(rawResistances)
			for ((rawResistedObject, rawPercentage) in resistanceMap) {
				val percentage = parseInt(rawPercentage)
				val resistedElement = content.stats.elements.find { it.rawName == rawResistedObject }
				if (resistedElement != null) {
					shiftResistances[myElement]!!.elements.add(ElementalResistance(resistedElement, percentage / 100f))
				} else {
					val resistedEffect = content.stats.statusEffects.find { it.flashName == rawResistedObject }!!
					shiftResistances[myElement]!!.effects.add(EffectResistance(resistedEffect, percentage))
				}
			}
		}
	}

	return Monster(
		name = name,
		model = model,
		className = parseFlashString(mdlMap["Class"]!!, "monster class")!!,
		type = content.stats.creatureTypes.find { it.flashName == typeName }!!,
		element = content.stats.elements.find { it.rawName == elementName }!!,
		baseStats = baseStats,
		playerStatModifier = playerStatModifier,
		hpPerLevel = parseInt(mdlMap["hpGrowth"]!!),
		attackPerLevelNumerator = attackPerLevelNumerator,
		attackPerLevelDenominator = attackPerLevelDenominator,
		critChance = parseInt(mdlMap["critical"]!!),
		experience = parseInt(mdlMap["EXP"]!!),
		loot = loot,
		plotLoot = plotLoot,
		dreamLoot = dreamLoot,
		weapon = parsePotentialEquipment(rawEquipmentMap["weapon"], content.items),
		shield = parsePotentialEquipment(rawEquipmentMap["shield"], content.items),
		helmet = parsePotentialEquipment(rawEquipmentMap["helmet"], content.items),
		armor = parsePotentialEquipment(rawEquipmentMap["armour"], content.items),
		accessory1 = parsePotentialEquipment(rawEquipmentMap["accs"], content.items),
		accessory2 = parsePotentialEquipment(rawEquipmentMap["accs2"], content.items),
		resistances = Resistances(elementalResistances, statusResistances),
		elementalShiftResistances = shiftResistances,
		attackEffects = attackEffects,
		initialEffects = initialEffects,
		actions = ArrayList(usedActions),
		strategies = strategies,
		meleeCounterAttacks = meleeCounterAttacks,
		rangedCounterAttacks = rangedCounterAttacks
	)
}
