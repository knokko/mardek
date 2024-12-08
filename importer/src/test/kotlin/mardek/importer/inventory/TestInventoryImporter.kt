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
		assertEquals("Blind", brightEyes.statusResistances[0].effect.niceName)
		assertEquals(100, brightEyes.statusResistances[0].chance)
	}

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

		assertEquals("_turquoise", gem.rawName)
		assertEquals(30, gem.power)
		assertEquals("Being a form of beryl, aquamarine is related to emeralds. However, whereas emeralds contain earth-elemental essence, aquamarine is brimming with the essence of the water element. It feels somehow wet...", aquamarine.description)
		assertEquals(2, equipment.skills.size)

		val whirlpool = equipment.skills[0] as ActiveSkill
		assertEquals("Whirlpool", whirlpool.name)
		assertEquals(aquamarine.element, whirlpool.element)

		val spirit1 = equipment.skills[1] as PassiveSkill
		assertEquals(1, spirit1.statModifiers.size)
		assertEquals(1, spirit1.statModifiers.find { it.stat.flashName == "SPR" }!!.adder)
	}

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
				"Paralysis", "Sleep", "Confusion", "Poison", "Blind", "Curse", "Numbness", "Silence"
		)
		assertEquals(resistedEffects.size, equipment.statusResistances.size)
		for (effect in resistedEffects) {
			assertEquals(10, equipment.statusResistances.find { it.effect.niceName == effect }!!.chance)
		}

		val gem = equipment.gem!!
		assertEquals(80, gem.power)
		assertEquals("_sapphire", gem.rawName)

		assertEquals(1, equipment.skills.size)
		assertEquals(1, (equipment.skills[0] as PassiveSkill).masteryModifier)
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
		assertEquals(-0.5f, consumable.addStatusEffects[0].effect.meleeDamageReduction)
		assertEquals(-0.5f, consumable.addStatusEffects[1].effect.rangedDamageReduction)

		assertEquals(rgb(220, 200, 200), consumable.particleColor)
		assertEquals("shield", consumable.particleEffect)
		assertEquals(0, consumable.restoreMana)
	}

	@Test
	fun testEtherOfQueens() {
		val ether = getItem("Ether of Queens")
		assertEquals("item", ether.type.flashName)
		assertEquals(3000, ether.cost)
		assertNull(ether.element)

		val consumable = ether.consumable!!
		assertFalse(consumable.isNegative)
		assertEquals(200, consumable.restoreMana)
		assertEquals("potion_ether2", consumable.particleEffect)
		assertEquals(rgb(100, 255, 255), consumable.particleColor)

		assertEquals("A higher quality ether, infused with the blood of a fairy queen or something like that. It restores 200MP.", ether.description)
	}

	@Test
	fun testAlchemistsFire() {
		val alchemistsFire = getItem("Alchemist's Fire")
		assertEquals("item", alchemistsFire.type.flashName)
		assertEquals("FIRE", alchemistsFire.element!!.properName)
		assertEquals(500, alchemistsFire.cost)

		val consumable = alchemistsFire.consumable!!
		assertTrue(consumable.isNegative)
		assertEquals("flame1", consumable.particleEffect)
		assertEquals(rgb(255, 200, 0), consumable.particleColor)

		val damage = consumable.damage!!
		assertEquals(50, damage.power)
		assertEquals(20, damage.spirit)
		assertEquals(alchemistsFire.element, damage.element)
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
	}

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
	}
}
