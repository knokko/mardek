package mardek.importer.inventory

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.compressor.Kim1Decompressor
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.EquipmentProperties
import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.Item
import mardek.assets.skill.*
import mardek.importer.combat.importCombatAssets
import mardek.importer.skills.importSkills
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.system.MemoryUtil.memCalloc
import org.lwjgl.system.MemoryUtil.memFree
import java.awt.Color
import java.nio.ByteBuffer
import javax.imageio.ImageIO

internal fun assertSpriteEquals(sheetName: String, x: Int, y: Int, compressedSprite: ByteBuffer) {
	val sheetInput = TestInventoryImporter::class.java.getResourceAsStream("itemsheet_$sheetName.png")!!
	val sheet = ImageIO.read(sheetInput)
	sheetInput.close()

	val expectedImage = sheet.getSubimage(x, y, 16, 16)

	val decompressor = Kim1Decompressor(compressedSprite)
	assertEquals(16, decompressor.width)
	assertEquals(16, decompressor.height)

	for (ox in 0 until 16) {
		for (oy in 0 until 16) {
			val expected = Color(expectedImage.getRGB(ox, oy), true)
			val actual = decompressor.getColor(ox, oy)
			assertEquals(expected.red, unsigned(red(actual)))
			assertEquals(expected.green, unsigned(green(actual)))
			assertEquals(expected.blue, unsigned(blue(actual)))
			assertEquals(expected.alpha, unsigned(alpha(actual)))
		}
	}

	memFree(compressedSprite)
}

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

	private fun assertSpriteEquals(sheetName: String, x: Int, y: Int, item: Item) {
		val compressedData = memCalloc(4 * item.sprite.data!!.size)
		for (value in item.sprite.data!!) compressedData.putInt(value)
		compressedData.position(0)

		assertSpriteEquals(sheetName, x, y, compressedData)
	}

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

		assertSpriteEquals("weapons", 352, 0, blade)
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

		assertSpriteEquals("armour", 256, 128, helmet)
	}

	@Test
	fun testSilverPendant() {
		val pendant = getItem("SilverPendant")
		val equipment = pendant.equipment!!
		assertNull(equipment.armor)
		assertNull(equipment.weapon)
		assertNull(pendant.consumable)

		assertEquals(1, equipment.stats.size)
		assertEquals(3, getStatModifier("MDEF", equipment))

		assertEquals(100, pendant.cost)
		assertEquals("accs", pendant.type.flashName)
		assertEquals("LIGHT", pendant.element!!.properName)

		assertEquals(1, equipment.elementalResistances.size)
		assertEquals("LIGHT", equipment.elementalResistances[0].element.properName)
		assertEquals(-0.15f, equipment.elementalResistances[0].modifier, margin)

		assertSpriteEquals("misc", 32, 0, pendant)
	}

	@Test
	fun testSilverAxe() {
		val axe = getItem("Silver Axe")
		val equipment = axe.equipment!!
		val weapon = equipment.weapon!!

		assertEquals("wepn", axe.type.flashName)
		assertEquals("GREATAXE", weapon.type.flashName)
		assertEquals(46, getStatModifier("ATK", equipment))
		assertEquals(4, weapon.critChance)
		assertEquals(95, weapon.hitChance)
		assertEquals(12000, axe.cost)

		assertEquals(1, weapon.raceBonuses.size)
		assertEquals("UNDEAD", weapon.raceBonuses[0].race.flashName)
		assertEquals(1f, weapon.raceBonuses[0].bonusFraction, margin)
		assertEquals("LIGHT", axe.element!!.properName)
		assertEquals("hit_2HSWORDS", weapon.hitSound)

		assertEquals(1, equipment.skills.size)
		val quarry = equipment.skills[0] as ReactionSkill
		assertEquals(ReactionSkillType.MeleeAttack, quarry.type)
		assertEquals(1, quarry.effectiveAgainst.size)

		assertSpriteEquals("weapons", 80, 48, axe)
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

		assertEquals("gemsplosion_turquoise", gem.particleEffect)
		assertEquals(30, gem.power)
		assertEquals(0, gem.inflictStatusEffects.size)
		assertEquals(0f, gem.drainHp)
		assertEquals("Being a form of beryl, aquamarine is related to emeralds. However, whereas emeralds contain earth-elemental essence, aquamarine is brimming with the essence of the water element. It feels somehow wet...", aquamarine.description)
		assertEquals(2, equipment.skills.size)

		val whirlpool = equipment.skills[0] as ActiveSkill
		assertEquals("Whirlpool", whirlpool.name)
		assertEquals(aquamarine.element, whirlpool.element)

		val spirit1 = equipment.skills[1] as PassiveSkill
		assertEquals(1, spirit1.statModifiers.size)
		assertEquals(1, spirit1.statModifiers.find { it.stat.flashName == "SPR" }!!.adder)

		assertSpriteEquals("misc", 128, 48, aquamarine)
	}

	@Test
	fun testAmethyst() {
		val amethyst = getItem("Amethyst")
		val gem = amethyst.equipment!!.gem!!
		assertEquals(40, gem.power)
		assertEquals("gemsplosion_amethyst", gem.particleEffect)
		assertEquals(0f, gem.drainHp, margin)

		assertEquals(1, gem.inflictStatusEffects.size)
		assertEquals("Confusion", gem.inflictStatusEffects[0].effect.niceName)
		assertEquals(50, gem.inflictStatusEffects[0].chance)

		assertSpriteEquals("misc", 176, 48, amethyst)
	}

	@Test
	fun testBloodOpal() {
		val bloodOpal = getItem("BloodOpal")
		val gem = bloodOpal.equipment!!.gem!!

		assertEquals(30, gem.power)
		assertEquals("gemsplosion_ruby", gem.particleEffect)
		assertEquals(0, gem.inflictStatusEffects.size)
		assertEquals(1f, gem.drainHp, margin)

		assertSpriteEquals("misc", 80, 48, bloodOpal)
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
				"Paralysis", "Sleep", "Confusion", "Poison", "Blindness", "Curse", "Numbness", "Silence"
		)
		assertEquals(resistedEffects.size, equipment.statusResistances.size)
		for (effect in resistedEffects) {
			assertEquals(10, equipment.statusResistances.find { it.effect.niceName == effect }!!.chance)
		}

		val gem = equipment.gem!!
		assertEquals(80, gem.power)
		assertEquals("gemsplosion_sapphire", gem.particleEffect)
		assertEquals(0, gem.inflictStatusEffects.size)
		assertEquals(0f, gem.drainHp, margin)

		assertEquals(1, equipment.skills.size)
		assertEquals(1, (equipment.skills[0] as PassiveSkill).masteryModifier)

		assertSpriteEquals("misc", 240, 48, eveningStar)
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
		assertSpriteEquals("misc", 304, 32, elixir)
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

		assertSpriteEquals("misc", 272, 32,  phoenixDown)
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

		assertSpriteEquals("misc", 320, 32, mirrilixir)
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
		assertSpriteEquals("misc", 128, 32, ether)
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

		assertSpriteEquals("misc", 384, 32, alchemistsFire)
	}

	@Test
	fun testNoxiousBomb() {
		val bomb = getItem("Noxious Bomb")
		assertEquals("DARK", bomb.element!!.properName)
		assertEquals(1000, bomb.cost)

		val consumable = bomb.consumable!!
		val sfx = consumable.addStatusEffects
		assertEquals(4, sfx.size)
		assertEquals(50, sfx.find { it.effect.niceName == "Blindness" }!!.chance)
		assertEquals(100, sfx.find { it.effect.niceName == "Confusion" }!!.chance)
		assertEquals(50, sfx.find { it.effect.niceName== "Sleep" }!!.chance)
		assertEquals(100, sfx.find { it.effect.niceName == "Poison" }!!.chance)

		assertNull(consumable.damage)
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

		assertSpriteEquals("misc", 0, 32, potion)
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

		assertSpriteEquals("misc", 512, 32, powerDrink)
	}

	@Test
	fun testMagicDrink() {
		val magicDrink = getItem("Magic Drink")
		assertEquals(2000, magicDrink.cost)

		val consumable = magicDrink.consumable!!
		assertEquals(200, consumable.restoreMana)
		assertEquals(0, consumable.restoreHealth)
		assertEquals("boost_ETHER", consumable.particleEffect)

		assertEquals(1, consumable.statModifiers.size)
		assertEquals("SPR", consumable.statModifiers[0].stat.flashName)
		assertEquals(10, consumable.statModifiers[0].minAdder)
		assertEquals(10, consumable.statModifiers[0].maxAdder)

		assertSpriteEquals("misc", 528, 32, magicDrink)
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

		assertSpriteEquals("misc", 192, 32, liquidSound)
	}
}
