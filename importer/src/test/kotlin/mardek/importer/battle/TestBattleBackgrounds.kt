package mardek.importer.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestBattleBackgrounds {

	@Test
	fun testBattleBackgrounds() {
		assertEquals(43, importBattleAssets().backgrounds.size)
	}
}
