package mardek.importer.inventory

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.EquipmentProperties
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.*
import mardek.importer.combat.importCombatAssets
import mardek.importer.skills.importSkills
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestInventoryImporter {

	private val margin = 1e-4f

	private lateinit var combatAssets: CombatAssets
	private lateinit var skillAssets: SkillAssets
	private lateinit var inventoryAssets: InventoryAssets

	@BeforeAll
	fun importItems() {
		combatAssets = importCombatAssets()
		skillAssets = importSkills(combatAssets, "mardek/importer/combat/skills.txt")
		inventoryAssets = importInventoryAssets(combatAssets, skillAssets, "mardek/importer/inventory/data.txt")
	}

	@Test
	fun testImportSword() {
		assertEquals("MARTIAL", inventoryAssets.weaponTypes.find { it.flashName == "SWORD" }!!.soundEffect)
	}

	@Test
	fun testImportGreatAxe() {
		assertNull(inventoryAssets.weaponTypes.find { it.flashName == "GREATAXE" }!!.soundEffect)
	}

	@Test
	fun testImportMediumArmor() {
		assertEquals("MEDIUM ARMOUR", inventoryAssets.armorTypes.find { it.key == "Ar2" }!!.name)
	}

	@Test
	fun testGems() {
		assertTrue(inventoryAssets.itemTypes.find { it.flashName == "gems" }!!.canStack)
	}

	@Test
	fun testWeapons() {
		assertFalse(inventoryAssets.itemTypes.find { it.flashName == "wepn" }!!.canStack)
	}

	private fun getItem(name: String) = inventoryAssets.items.find { it.flashName == name }!!

	private fun getStatModifier(name: String, equipment: EquipmentProperties) = equipment.stats.find {
		it.stat.flashName == name
	}!!.adder

	@Test
	fun testCursedBlade() {
		val blade = getItem("Cursed Blade")
		val equipment = blade.equipment!!
		val weapon = equipment.weapon!!

		assertEquals("wepn", blade.type.flashName)
		assertEquals("SWORD", weapon.type.flashName)
		assertEquals(45, getStatModifier("ATK", equipment))
		assertEquals(5, getStatModifier("STR", equipment))
		assertEquals(-5, getStatModifier("VIT", equipment))
		assertEquals(-5, getStatModifier("SPR", equipment))
		assertEquals(10, weapon.critChance)
		assertEquals(97, weapon.hitChance)
		assertEquals(3000, blade.cost)
		assertEquals("DARK", blade.element!!.properName)

		assertEquals(1, weapon.addEffects.size)
		assertEquals("Curse", weapon.addEffects[0].effect.niceName)
		assertEquals(100, weapon.addEffects[0].chance)
		assertEquals(1f, weapon.hpDrain, margin)

		assertEquals(2, equipment.autoEffects.size)
		assertNotNull(equipment.autoEffects.find { it.niceName == "Curse" })
		assertNotNull(equipment.autoEffects.find { it.niceName == "Bleed" })

		assertEquals(1, equipment.skills.size)
		val bleed = equipment.skills[0] as ReactionSkill
		assertEquals(1, bleed.addStatusEffects.size)
		assertEquals("Bleed", bleed.addStatusEffects[0].effect.niceName)
		assertEquals(20, bleed.addStatusEffects[0].chance)

		// {name:"Cursed Blade",type:"wepn",wpnType:"SWORD",atk:45,critical:10,hit:97,cost:3000,elem:"DARK",stfx:{CRS:100},HP_DRAIN:true,effects:[["STR",5],["VIT",-5],["SPR",-5],["AUTO_STFX","CRS"],["AUTO_STFX","BLD"]],desc:"An ancient blade, cursed by some powerful wizard. Whoever wields it is afflicted with the Curse status effect, but so are any enemies that it strikes. It also drains the energy of its victims, but causes its wielder to bleed constantly.",skills:[["R:P_ATK","P+Bleed 20%"]]}
	}

	@Test
	fun testGoldenHelmet() {
		val helmet = getItem("Golden M Helm")
		val equipment = helmet.equipment!!
		assertEquals("helm", helmet.type.flashName)
		assertEquals(6, getStatModifier("DEF", equipment))
		assertEquals(3, getStatModifier("MDEF", equipment))
		assertEquals(20000, helmet.cost)
		assertEquals("FULL HELM", equipment.armor!!.type.name)

		assertEquals(1, getStatModifier("STR", equipment))
		assertEquals(1, getStatModifier("VIT", equipment))
		assertEquals(1, getStatModifier("SPR", equipment))
		assertEquals(1, getStatModifier("AGL", equipment))

		assertEquals(1, equipment.elementalResistances.size)
		assertEquals("LIGHT", equipment.elementalResistances[0].element.properName)
		assertEquals(-0.3f, equipment.elementalResistances[0].modifier, margin)
		assertEquals(1, equipment.elementalBonuses.size)
		assertEquals("LIGHT", equipment.elementalBonuses[0].element.properName)
		assertEquals(0.2f, equipment.elementalBonuses[0].modifier, margin)

		assertEquals("Mardek", equipment.onlyUser)
		assertEquals("A golden helmet fashioned specifically for Mardek. The M-shaped visor looks sort of silly, but Mardek insists on it because he doesn\'t take life too seriously anyway and thinks it\'s rather fitting. ON HIS HEAD!!! ...Get it?!?1 Never mind.", helmet.description)

		assertEquals(2, equipment.skills.size)
		val increaseDamage = equipment.skills[0] as ReactionSkill
		assertEquals(ReactionSkillType.RangedAttack, increaseDamage.type)
		assertEquals(0.2f, increaseDamage.addDamageFraction, margin)
		val brightEyes = equipment.skills[1] as PassiveSkill
		assertEquals(1, brightEyes.statusResistances.size)
		assertEquals("Blindness", brightEyes.statusResistances[0].effect.niceName)
		assertEquals(100, brightEyes.statusResistances[0].chance)

		// {name:"Golden M Helm",type:"helm",def:6,mdef:3,cost:20000,amrType:"H2",elem:"LIGHT",effects:[["STR",1],["VIT",1],["SPR",1],["AGL",1],["R_ELEM","LIGHT",30],["EMPOWER","LIGHT",20]],only_user:"Mardek",desc:"A golden helmet fashioned specifically for Mardek. The M-shaped visor looks sort of silly, but Mardek insists on it because he doesn\'t take life too seriously anyway and thinks it\'s rather fitting. ON HIS HEAD!!! ...Get it?!?1 Never mind.",skills:[["R:M_ATK","M DMG+20%"],["R:PASSIVE","Bright Eyes"]]}
	}

	// TODO {name:"SilverPendant",type:"accs",def:0,mdef:3,cost:100,effects:[["R_ELEM","LIGHT",15]],elem:"LIGHT",desc:"A delicate silver piece of jewellery. Wear it around your neck.",skills:[]}
	// TODO {name:"Silver Axe",type:"wepn",wpnType:"GREATAXE",atk:46,critical:4,hit:95,cost:12000,typeBonus:{UNDEAD:2},elem:"LIGHT",hit_sfx:"hit_2HSWORDS",desc:"A delicately ornate axe forged from the shiniest silver. Like all silver weapons, it\'s effective against undead.",skills:[["R:P_ATK","Quarry: UNDEAD"]]}

	@Test
	fun testAquamarine() {
		val aquamarine = getItem("Aquamarine")
		val equipment = aquamarine.equipment!!
		val gem = equipment.gem!!

		assertEquals("gems", aquamarine.type.flashName)
		assertEquals(800, aquamarine.cost)
		assertEquals("WATER", aquamarine.element!!.properName)

		assertEquals(1, equipment.elementalResistances.size)
		assertEquals(aquamarine.element, equipment.elementalResistances[0].element)
		assertEquals(-0.1f, equipment.elementalResistances[0].modifier, margin)
		assertEquals(1, equipment.elementalBonuses.size)
		assertEquals(aquamarine.element, equipment.elementalBonuses[0].element)
		assertEquals(0.1f, equipment.elementalBonuses[0].modifier, margin)

		//assertEquals("_turquoise", gem.rawName) TODO
		assertEquals(30, gem.power)
		assertEquals("Being a form of beryl, aquamarine is related to emeralds. However, whereas emeralds contain earth-elemental essence, aquamarine is brimming with the essence of the water element. It feels somehow wet...", aquamarine.description)
		assertEquals(2, equipment.skills.size)

		val whirlpool = equipment.skills[0] as ActiveSkill
		assertEquals("Whirlpool", whirlpool.name)
		assertEquals(aquamarine.element, whirlpool.element)

		val spirit1 = equipment.skills[1] as PassiveSkill
		assertEquals(1, spirit1.statModifiers.size)
		assertEquals(1, spirit1.statModifiers.find { it.stat.flashName == "SPR" }!!.adder)

		// {name:"Aquamarine",type:"gems",cost:800,elem:"WATER",effects:[["R_ELEM","WATER",10],["EMPOWER","WATER",10]],spell:["_turquoise",30],desc:"Being a form of beryl, aquamarine is related to emeralds. However, whereas emeralds contain earth-elemental essence, aquamarine is brimming with the essence of the water element. It feels somehow wet...",skills:[["Shm","Whirlpool"],["R:PASSIVE","SPR+1"]]}
	}

	// TODO {name:"Amethyst",type:"gems",cost:1400,elem:"AIR",effects:[["R_STATUS","CNF",30]],reagent:[["stfx","CNF",15]],spell:["_amethyst",40,{CNF:50}],desc:"A coloured variety of quartz, usually used to adorn jewellery or armour. It\'s superstitiously believed to protect from intoxication, but this has never been proven in the slightest.",skills:[["R:PASSIVE","Clarity"]]}
	// TODO {name:"BloodOpal",type:"gems",def:1,cost:3000,elem:"DARK",effects:[["R_STATUS","BLD",50]],reagent:[["DRAIN"]],spell:["_ruby",30,null,{DRAIN:1}],desc:"A common but powerful gemstone that\'s said to be the crystallised blood of a warrior of some ancient race, slain in some horrendous bloody battle in the distant past.",skills:[["R:P_ATK","Drain HP 10%"]]}

	@Test
	fun testEveningStar() {
		val eveningStar = getItem("EveningStar")
		assertEquals("gems", eveningStar.type.flashName)
		assertEquals(40000, eveningStar.cost)
		assertEquals("THAUMA", eveningStar.element!!.properName)

		val equipment = eveningStar.equipment!!
		assertEquals(2, equipment.stats.size)
		assertEquals(1, getStatModifier("DEF", equipment))
		assertEquals(1, getStatModifier("MDEF", equipment))

		val resistedEffects = arrayOf(
				"Paralysis", "Sleep", "Confusion", "Poison", "Blindness", "Curse", "Numbness", "Silence"
		)
		assertEquals(resistedEffects.size, equipment.statusResistances.size)
		for (effect in resistedEffects) {
			assertEquals(10, equipment.statusResistances.find { it.effect.niceName == effect }!!.chance)
		}

		val gem = equipment.gem!!
		assertEquals(80, gem.power)
		//assertEquals("_sapphire", gem.rawName) TODO

		assertEquals(1, equipment.skills.size)
		assertEquals(1, (equipment.skills[0] as PassiveSkill).masteryModifier)

		// {name:"EveningStar",type:"gems",cost:40000,elem:"THAUMA",def:1,mdef:1,effects:[["R_STATUS","PAR",10],["R_STATUS","SLP",10],["R_STATUS","CNF",10],["R_STATUS","PSN",10],["R_STATUS","DRK",10],["R_STATUS","CRS",10],["R_STATUS","NUM",10],["R_STATUS","SIL",10]],spell:["_sapphire",80],desc:"A gem that\'s said to have come from a rock that fell from the sky.",skills:[["R:PASSIVE","Double AP"]]}
	}

	@Test
	fun testElixir() {
		val elixir = getItem("Elixir")
		assertEquals("item", elixir.type.flashName)
		assertEquals(9999, elixir.cost)
		assertNull(elixir.element)

		val consumable = elixir.consumable!!
		assertTrue(consumable.isFullCure)
		assertEquals("potion5", consumable.particleEffect)
		assertEquals(rgb(255, 220, 20), consumable.particleColor)

		assertEquals("An amazing brew made from the tears of a goddess, or something astounding like that. It restores all HP and MP.", elixir.description)

		// {name:"Elixir",type:"item",cost:9999,action:["fullcure"],pfx:"potion5",rgb:[255,220,20],pspray:{pfx:"sprayfall",DMG:["SPECIAL","fullcure"]},desc:"An amazing brew made from the tears of a goddess, or something astounding like that. It restores all HP and MP."}
	}

	@Test
	fun testPhoenixDown() {
		val phoenixDown = getItem("PhoenixDown")
		assertEquals("item", phoenixDown.type.flashName)
		assertEquals(500, phoenixDown.cost)
		assertNull(phoenixDown.element)

		val consumable = phoenixDown.consumable!!
		assertEquals(0.5f, consumable.revive, margin)
		assertEquals("phoenixdown", consumable.particleEffect)
		assertEquals(rgb(255, 200, 50), consumable.particleColor)

		// {name:"PhoenixDown",type:"item",cost:500,action:["life",0.5],pfx:"phoenixdown",rgb:[255,200,50],desc:"A soft, warm feather from the underbelly of a phoenix. It glows with a pleasant orange light. Pressing it to the forehead of a KO\'d ally will cause them to be revived with half HP."}
	}

	@Test
	fun testMirrilixir() {
		val mirrilixir = getItem("Mirrilixir")
		assertEquals("item", mirrilixir.type.flashName)
		assertEquals(5000, mirrilixir.cost)
		assertNull(mirrilixir.element)

		val consumable = mirrilixir.consumable!!
		assertEquals(2, consumable.addStatusEffects.size)
		for (effect in consumable.addStatusEffects) assertEquals(100, effect.chance)
		assertEquals(0.5f, consumable.addStatusEffects[0].effect.meleeDamageReduction)
		assertEquals(0.5f, consumable.addStatusEffects[1].effect.rangedDamageReduction)

		assertEquals(rgb(220, 220, 220), consumable.particleColor)
		assertEquals("shield", consumable.particleEffect)
		assertEquals(0, consumable.restoreMana)

		// {name:"Mirrilixir",type:"item",cost:5000,action:["spell",{pow:0,SPR:15,elem:"LIGHT",stfx:{PSH:100,MSH:100},pfx:"shield"}],rgb:[220,220,220],pspray:{pfx:"sprayfall",DMG:null,stfx:{PSH:100,MSH:100},lasts:3,special:null},desc:"A potent potion with a silvery, reflective texture, like liquid mercury. Drinking it gives the imbiber P.Shield and M.Shield status effects."}
	}

	@Test
	fun testEtherOfQueens() {
		val ether = getItem("Ether of Queens")
		assertEquals("item", ether.type.flashName)
		assertEquals(3000, ether.cost)
		assertNull(ether.element)

		val consumable = ether.consumable!!
		assertEquals(200, consumable.restoreMana)
		assertEquals("potion_ether2", consumable.particleEffect)
		assertEquals(rgb(100, 255, 255), consumable.particleColor)

		assertEquals("A higher quality ether, infused with the blood of a fairy queen or something like that. It restores 200MP.", ether.description)
		// {name:"Ether of Queens",type:"item",cost:3000,action:["r_MP",200],pfx:"potion_ether2",rgb:[100,255,255],pspray:{pfx:"sprayfall",DMG:["SPECIAL","set_dmg",-200],affectMP:true},desc:"A higher quality ether, infused with the blood of a fairy queen or something like that. It restores 200MP."}
	}

	@Test
	fun testAlchemistsFire() {
		val alchemistsFire = getItem("Alchemist's Fire")
		assertEquals("item", alchemistsFire.type.flashName)
		assertEquals("FIRE", alchemistsFire.element!!.properName)
		assertEquals(500, alchemistsFire.cost)

		val consumable = alchemistsFire.consumable!!
		assertEquals("flame1", consumable.particleEffect)
		assertEquals(rgb(255, 200, 0), consumable.particleColor)

		val damage = consumable.damage!!
		assertEquals(50, damage.power)
		assertEquals(20, damage.spirit)
		assertEquals(alchemistsFire.element, damage.element)

		// {name:"Alchemist\'s Fire",type:"item",elem:"FIRE",cost:500,hurtful:true,action:["spell",{pow:50,SPR:20,elem:"FIRE",pfx:"flame1"}],rgb:[255,200,0],desc:"A magical potion that explodes if under stress. Throwing it at an enemy will inflict fire damage."}
	}

	@Test
	fun testPotion() {
		val potion = getItem("Potion")
		assertEquals("item", potion.type.flashName)
		assertEquals(30, potion.cost)
		assertNull(potion.element)

		val consumable = potion.consumable!!
		assertEquals(100, consumable.restoreHealth)
		assertEquals("potion", consumable.particleEffect)
		assertEquals(rgb(100, 160, 220), consumable.particleColor)

		// {name:"Potion",type:"item",cost:30,action:["r_HP",100],pfx:"potion",rgb:[100,160,220],pspray:{pfx:"sprayfall",DMG:["SPECIAL","set_dmg",-100]},desc:"A concoction made from bleach, vinegar, urine, and a pinch of brown sugar. Its aroma alone can melt steel, but fortunately for you, all it does in this game is restore 100HP."}
	}

	@Test
	fun testPowerDrink() {
		val powerDrink = getItem("Power Drink")
		assertEquals("item", powerDrink.type.flashName)
		assertEquals(2000, powerDrink.cost)

		val consumable = powerDrink.consumable!!
		assertEquals(1000, consumable.restoreHealth)
		assertEquals(1, consumable.statModifiers.size)

		val increaseStrength = consumable.statModifiers[0]
		assertEquals(10, increaseStrength.minAdder)
		assertEquals(10, increaseStrength.maxAdder)
		assertEquals("STR", increaseStrength.stat.flashName)

		assertEquals("boost", consumable.particleEffect)
		assertEquals(rgb(255, 100, 0), consumable.particleColor)

		// {name:"Power Drink",type:"item",cost:2000,action:["spell",{set_dmg:-1000,SPR:15,elem:"FIRE",stat_mod:{STR:10},special:{HEALING:1},pfx:"boost"}],rgb:[255,100,0],pspray:{pfx:"sprayfall",DMG:["SPECIAL","set_dmg",-500],stat_mod:{STR:5}},desc:"A special shaman\'s brew, which restores 1000 HP and increases STR by 10 for the duration of the battle."}
	}

	// TODO {name:"Magic Drink",type:"item",cost:2000,action:["spell",{set_dmg:-200,SPR:15,affectMP:true,elem:"WATER",stat_mod:{SPR:10},special:{HEALING:1},pfx:"boost_ETHER"}],rgb:[255,100,0],pspray:{pfx:"sprayfall",affectMP:true,DMG:["SPECIAL","set_dmg",-100],stat_mod:{SPR:5}},desc:"A special shaman\'s brew, which restores 200 MP and increases SPR by 10 for the duration of the battle."}

	@Test
	fun testLiquidSound() {
		val liquidSound = getItem("LiquidSound")
		assertEquals("item", liquidSound.type.flashName)
		assertEquals(20, liquidSound.cost)

		val consumable = liquidSound.consumable!!
		assertEquals(1, consumable.removeStatusEffects.size)
		assertEquals(100, consumable.removeStatusEffects[0].chance)
		assertEquals("Silence", consumable.removeStatusEffects[0].effect.niceName)

		assertEquals("cleanse", consumable.particleEffect)
		assertEquals(rgb(100, 0, 155), consumable.particleColor)

		// {name:"LiquidSound",type:"item",cost:20,action:["h_status","SIL"],pfx:"cleanse",rgb:[100,0,155],pspray:{pfx:"sprayfall",DMG:null,stfx:{PSN:SIL}},desc:"This vial contains a vaguely liquid-like substance that cures silence when drunk. And even when sober! Wow!\nIf you put it to your ear, you can hear a neverending tune."}
	}
}
