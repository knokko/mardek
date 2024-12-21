package mardek.state.ingame.menu

import mardek.assets.inventory.EquipmentSlotType
import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.characters.CharacterState
import mardek.state.ingame.inventory.ItemStack

class InventoryTab(private val state: CampaignState): InGameMenuTab(true) {

	var partyIndex = 0

	var pickedUpItem: ItemReference? = null
	var hoveringItem: ItemReference? = null

	var renderItemsStartX = -1
	var renderItemsStartY = -1
	var renderItemSlotSize = -1

	var mouseX = -1
	var mouseY = -1

	override fun getText() = "Inventory"

	private fun validatePartyIndex() {
		if (state.characterSelection.party[partyIndex] == null) {
			partyIndex = state.characterSelection.party.indexOfFirst { it != null }
			if (partyIndex == -1) throw IllegalStateException("Party must have at least 1 character at all times")
		}
	}

	override fun processKeyPress(key: InputKey, soundQueue: SoundQueue) {
		if ((inside && key == InputKey.Interact) || key == InputKey.Click) {
			if (!inside) {
				inside = true
				soundQueue.insert("click-confirm")
			}

			if (hoveringItem != null) {
				val oldItem = hoveringItem!!.get()
				if (oldItem != null) {
					if (pickedUpItem == null) {
						pickedUpItem = hoveringItem
						soundQueue.insert("click-confirm")
					} else if (hoveringItem == pickedUpItem) {
						pickedUpItem = null
						soundQueue.insert("click-cancel")
					} else {
						hoveringItem!!.set(pickedUpItem!!.get())
						pickedUpItem!!.set(oldItem)
						soundQueue.insert("click-confirm")
					}
				} else if (pickedUpItem != null) {
					hoveringItem!!.set(pickedUpItem!!.get())
					pickedUpItem!!.set(null)
					pickedUpItem = null
					soundQueue.insert("click-cancel")
				}
			}
		}

		val party = state.characterSelection.party
		val oldPartyIndex = partyIndex
		if (key == InputKey.MoveUp) {
			partyIndex -= 1
			while (partyIndex >= 0 && party[partyIndex] == null) partyIndex -= 1
			if (partyIndex < 0) partyIndex = party.indexOfLast { it != null }
			if (partyIndex != oldPartyIndex) soundQueue.insert("menu-party-scroll")
		}

		if (key == InputKey.MoveDown) {
			partyIndex += 1
			while (partyIndex < party.size && party[partyIndex] == null) partyIndex += 1
			if (partyIndex >= party.size) partyIndex = party.indexOfFirst { it != null }
			if (partyIndex != oldPartyIndex) soundQueue.insert("menu-party-scroll")
		}

		super.processKeyPress(key, soundQueue)
	}

	override fun processMouseMove(event: MouseMoveEvent) {
		validatePartyIndex()
		val assetCharacter = state.characterSelection.party[partyIndex]!!
		val currentCharacter = state.characterStates[assetCharacter] ?: throw IllegalStateException(
			"Character ${assetCharacter.name} doesn't have a state"
		)
		if (renderItemSlotSize <= 0) return
		hoveringItem = null

		if (event.newX >= renderItemsStartX && event.newY >= renderItemsStartY) {
			val slotX = (event.newX - renderItemsStartX) / renderItemSlotSize
			val slotY = (event.newY - renderItemsStartY) / renderItemSlotSize
			if (slotX < 8 && slotY < 8) hoveringItem = ItemReference(currentCharacter, slotX + 8 * slotY)
		}

		mouseX = event.newX
		mouseY = event.newY
	}
}

class ItemReference(val character: CharacterState, val slotIndex: Int) {

	override fun toString() = if (slotIndex >= 0) "inventory slot $slotIndex" else "equipment slot ${equipmentIndex()}"

	override fun equals(other: Any?) = other is ItemReference && character === other.character && slotIndex == other.slotIndex

	override fun hashCode() = character.hashCode() + 31 * slotIndex

	private fun equipmentIndex() = -slotIndex - 1

	fun get() = if (slotIndex >= 0) character.inventory[slotIndex] else {
		val item = character.equipment[equipmentIndex()]
		if (item != null) ItemStack(item, 1) else null
	}

	fun set(newStack: ItemStack?) {
		if (slotIndex < 0) {
			if (newStack == null) character.equipment[equipmentIndex()] = null
			else if (newStack.amount == 1) character.equipment[equipmentIndex()] = newStack.item
			else throw IllegalArgumentException("Equipment can't stack: attempted to put $newStack")
		} else character.inventory[slotIndex] = newStack
	}

	fun getEquipmentType() = when (slotIndex) {
		-1 -> EquipmentSlotType.MainHand
		-2 -> EquipmentSlotType.OffHand
		-3 -> EquipmentSlotType.Head
		-4 -> EquipmentSlotType.Body
		-5 -> EquipmentSlotType.Accessory
		-6 -> EquipmentSlotType.Accessory
		else -> null
	}
}
