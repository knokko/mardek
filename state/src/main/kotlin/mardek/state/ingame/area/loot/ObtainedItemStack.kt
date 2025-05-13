package mardek.state.ingame.area.loot

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.characters.CharacterState

class ObtainedItemStack(
	val itemStack: ItemStack?,
	val plotItem: PlotItem?,
	val party: Array<PlayableCharacter?>,
	val characters: Map<PlayableCharacter, CharacterState>,
	val close: (Boolean) -> Unit,
) {
	var partyIndex = party.indexOfFirst { it != null }

	fun processKeyPress(key: InputKey, sounds: FixedSoundEffects, soundQueue: SoundQueue) {
		if (key == InputKey.Interact) {
			if (plotItem != null) {
				close(true)
				return
			}

			val character = party[partyIndex]!!
			val inventory = characters[character]!!.inventory

			var done = false
			if (itemStack!!.item.type.canStack) {
				for ((index, existingStack) in inventory.withIndex()) {
					if (existingStack != null && existingStack.item == itemStack.item) {
						inventory[index] = ItemStack(itemStack.item, existingStack.amount + itemStack.amount)
						done = true
						break
					}
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
			while (partyIndex >= 0 && party[partyIndex] == null) partyIndex -= 1
			if (partyIndex == -1) partyIndex = party.indexOfLast { it != null }
		}

		if (key == InputKey.MoveRight) {
			partyIndex += 1
			while (partyIndex < party.size && party[partyIndex] == null) partyIndex += 1
			if (partyIndex == party.size) partyIndex = party.indexOfFirst { it != null }
		}

		if (partyIndex != oldPartyIndex) soundQueue.insert(sounds.ui.scroll)
	}
}
