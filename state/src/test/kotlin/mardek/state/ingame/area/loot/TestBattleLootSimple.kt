package mardek.state.ingame.area.loot

import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.state.UsedPartyMember
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestBattleLootSimple {

	@Test
	fun testSelectGetAllInitially() {
		val loot = BattleLoot(
			0,
			arrayListOf(ItemStack(Item(), 1)),
			ArrayList(0),
			ArrayList(0),
			"hi",
			listOf(UsedPartyMember(0, PlayableCharacter(), CharacterState())),
		)
		assertEquals(BattleLoot.SelectedGetAll, loot.selectedElement)
	}

	@Test
	fun testSelectFinishInitially() {
		val loot = BattleLoot(
			0,
			ArrayList(0),
			ArrayList(0),
			ArrayList(0),
			"hi",
			listOf(UsedPartyMember(0, PlayableCharacter(), CharacterState())),
		)
		assertEquals(BattleLoot.SelectedFinish, loot.selectedElement)
	}
}
