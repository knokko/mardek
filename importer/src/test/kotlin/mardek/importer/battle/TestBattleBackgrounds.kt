package mardek.importer.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestBattleBackgrounds {

	@Test
	fun testBattleBackgrounds() {
		assertEquals(20, importBattleAssets().backgrounds.size)
	}
}