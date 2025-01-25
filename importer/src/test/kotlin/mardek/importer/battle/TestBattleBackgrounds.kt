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
		val itemAssets = importInventoryAssets(combatAssets, importSkills(combatAssets))
		assertEquals(43, importBattleAssets(combatAssets, itemAssets, null).backgrounds.size)
	}
}
