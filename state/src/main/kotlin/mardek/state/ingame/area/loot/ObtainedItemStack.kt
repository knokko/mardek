package mardek.state.ingame.area.loot

import mardek.content.audio.FixedSoundEffects
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.UsedPartyMember
import mardek.state.WholeParty

class ObtainedItemStack(
	val itemStack: ItemStack?,
	val plotItem: PlotItem?,
	val usedParty: List<UsedPartyMember>,
	val fullParty: WholeParty,
	val close: (Boolean) -> Unit,
) {
	var partyIndex = usedParty[0].index

	fun processKeyPress(key: InputKey, sounds: FixedSoundEffects, soundQueue: SoundQueue) {
		if (key == InputKey.Interact) {
			if (plotItem != null) {
				close(true)
				return
			}

			val (_, characterState) = fullParty[partyIndex]!!
			val inventory = characterState.inventory

			var done = false
			for ((index, existingStack) in inventory.withIndex()) {
				if (existingStack != null && existingStack.item == itemStack!!.item) {
					inventory[index] = ItemStack(itemStack.item, existingStack.amount + itemStack.amount)
					done = true
					break
				}
			}

			if (!done) {
				for ((index, existingStack) in inventory.withIndex()) {
					if (existingStack == null) {
						inventory[index] = itemStack
						done = true
						break
					}
				}
			}

			if (done) close(true)
			else soundQueue.insert(sounds.ui.clickReject)
		}

		if (key == InputKey.Cancel) close(false)

		val oldPartyIndex = partyIndex
		if (key == InputKey.MoveLeft) {
			partyIndex -= 1
			while (partyIndex >= 0 && fullParty[partyIndex] == null) partyIndex -= 1
			if (partyIndex == -1) partyIndex = usedParty.last().index
		}

		if (key == InputKey.MoveRight) {
			partyIndex += 1
			while (partyIndex < fullParty.size && fullParty[partyIndex] == null) partyIndex += 1
			if (partyIndex == fullParty.size) partyIndex = usedParty[0].index
		}

		if (partyIndex != oldPartyIndex) soundQueue.insert(sounds.ui.scroll1)
	}
}
