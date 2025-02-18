package mardek.importer.battle

import mardek.importer.combat.importCombatAssets
import mardek.importer.inventory.importInventoryAssets
import mardek.importer.skills.importSkills
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestBattleBackgrounds {

	@Test
	fun testBattleBackgrounds() {
		val combatAssets = importCombatAssets()
		val skillAssets = importSkills(combatAssets)
		val itemAssets = importInventoryAssets(combatAssets, skillAssets)
		assertEquals(43, importBattleAssets(combatAssets, itemAssets, skillAssets, null).backgrounds.size)
	}
}
