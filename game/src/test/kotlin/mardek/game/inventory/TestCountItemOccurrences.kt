package mardek.game.inventory

import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import org.junit.jupiter.api.Assertions

object TestCountItemOccurrences {

	fun testMixed(instance: TestingInstance) {
		instance.apply {
			val amethyst = content.items.items.find { it.displayName == "Amethyst" }!!
			val elixir = content.items.items.find { it.displayName == "Elixir" }!!
			val ruby = content.items.items.find { it.displayName == "Ruby" }!!
			val campaign = simpleCampaignState()

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.equipment[heroMardek.characterClass.equipmentSlots[4]] = amethyst
			mardekState.equipment[heroMardek.characterClass.equipmentSlots[5]] = ruby

			mardekState.inventory[4] = ItemStack(amethyst, 1)
			mardekState.inventory[20] = ItemStack(amethyst, 12)
			mardekState.inventory[5] = ItemStack(elixir, 5)

			Assertions.assertEquals(14, mardekState.countItemOccurrences(amethyst))
		}
	}
}
