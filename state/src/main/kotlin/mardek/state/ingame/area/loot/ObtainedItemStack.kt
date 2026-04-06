package mardek.state.ingame.area.loot

import mardek.content.audio.FixedSoundEffects
import mardek.content.inventory.ItemStack
import mardek.content.inventory.PlotItem
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.UsedPartyMember
import mardek.state.ingame.WholeParty

/**
 * An instance of this class will be created when the player opens a chest containing an item. When this
 * happens, the [mardek.state.ingame.area.AreaState.suspension] will be set to an instance of
 * [mardek.state.ingame.area.AreaSuspensionOpeningChest]. This will suspend the area until the player has either given
 * the item to one of its characters, or until the player closes the chest without taking the item.
 */
class ObtainedItemStack(

	/**
	 * The item that is in the chest, or `null` if the chest contains something else (e.g. a plot item)
	 */
	val itemStack: ItemStack?,

	/**
	 * The plot item that is in the chest, or `null` if the chest contains something else (usually a regular item)
	 */
	val plotItem: PlotItem?,

	/**
	 * The result of [mardek.state.ingame.CampaignState.usedPartyMembers]
	 */
	val usedParty: List<UsedPartyMember>,

	private val fullParty: WholeParty,

	private val close: (Boolean) -> Unit,
) {

	/**
	 * The index of the currently-selected party member (into [fullParty]). When the player presses the Interact key,
	 * the item will be put in the inventory of the party member with this index.
	 */
	var partyIndex = usedParty[0].index

	/**
	 * This method should be invoked whenever an `InputKeyEvent` with `didPress = true` is received while the player
	 * is looting a chest. It should be invoked by [mardek.state.ingame.area.AreaState.processChestKeyEvent].
	 */
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
