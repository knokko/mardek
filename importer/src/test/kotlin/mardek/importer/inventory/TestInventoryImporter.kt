package mardek.importer.inventory

import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets
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
}
