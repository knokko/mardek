package mardek.importer.inventory

import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.Item
import mardek.importer.combat.importCombatAssets
import mardek.importer.skills.importSkills
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.system.MemoryUtil.memCalloc

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestItemExporter {

	private lateinit var inventoryAssets: InventoryAssets
	private lateinit var itemSprites: IntArray

	@BeforeAll
	fun importItems() {
		val combatAssets = importCombatAssets()
		val skillAssets = importSkills(combatAssets, "mardek/importer/combat/skills.txt")
		inventoryAssets = importInventoryAssets(combatAssets, skillAssets, "mardek/importer/inventory/data.txt")
		itemSprites = exportItemTextures(inventoryAssets)
	}

	private fun assertSpriteEquals(sheetName: String, x: Int, y: Int, item: Item) {
		val compressedData = memCalloc(4 * itemSprites.size)
		for (value in itemSprites) compressedData.putInt(value)
		compressedData.position(4 * item.spriteIndex)

		assertSpriteEquals(sheetName, x, y, compressedData)
	}

	private fun getItem(name: String) = inventoryAssets.items.find { it.flashName == name }!!

	@Test
	fun testCursedBlade() {
		assertSpriteEquals("weapons", 352, 0, getItem("Cursed Blade"))
	}

	@Test
	fun testGoldenHelmet() {
		assertSpriteEquals("armour", 256, 128, getItem("Golden M Helm"))
	}

	@Test
	fun testSilverPendant() {
		assertSpriteEquals("misc", 32, 0, getItem("SilverPendant"))
	}

	@Test
	fun testSilverAxe() {
		assertSpriteEquals("weapons", 80, 48, getItem("Silver Axe"))
	}

	@Test
	fun testAquamarine() {
		assertSpriteEquals("misc", 128, 48, getItem("Aquamarine"))
	}

	@Test
	fun testAmethyst() {
		assertSpriteEquals("misc", 176, 48, getItem("Amethyst"))
	}

	@Test
	fun testBloodOpal() {
		assertSpriteEquals("misc", 80, 48, getItem("BloodOpal"))
	}

	@Test
	fun testEveningStar() {
		assertSpriteEquals("misc", 240, 48, getItem("EveningStar"))
	}

	@Test
	fun testElixir() {
		assertSpriteEquals("misc", 304, 32, getItem("Elixir"))
	}

	@Test
	fun testPhoenixDown() {
		assertSpriteEquals("misc", 272, 32,  getItem("PhoenixDown"))
	}

	@Test
	fun testMirrilixir() {
		assertSpriteEquals("misc", 320, 32, getItem("Mirrilixir"))
	}

	@Test
	fun testEtherOfQueens() {
		assertSpriteEquals("misc", 128, 32, getItem("Ether of Queens"))
	}

	@Test
	fun testAlchemistsFire() {
		assertSpriteEquals("misc", 384, 32, getItem("Alchemist's Fire"))
	}

	@Test
	fun testPotion() {
		assertSpriteEquals("misc", 0, 32, getItem("Potion"))
	}

	@Test
	fun testPowerDrink() {
		assertSpriteEquals("misc", 512, 32, getItem("Power Drink"))
	}

	@Test
	fun testMagicDrink() {
		assertSpriteEquals("misc", 528, 32, getItem("Magic Drink"))
	}

	@Test
	fun testLiquidSound() {
		assertSpriteEquals("misc", 192, 32, getItem("LiquidSound"))
	}
}
