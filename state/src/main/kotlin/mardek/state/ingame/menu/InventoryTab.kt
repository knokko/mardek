package mardek.state.ingame.menu

import mardek.assets.characters.PlayableCharacter
import mardek.assets.inventory.EquipmentSlotType
import mardek.assets.inventory.Item
import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.characters.CharacterState
import mardek.assets.inventory.ItemStack

class InventoryTab(private val state: CampaignState): InGameMenuTab(true) {

	var partyIndex = 0
	var descriptionIndex = 0

	var pickedUpItem: ItemReference? = null
	var hoveringItem: ItemReference? = null

	var renderItemsStartX = -1
	var renderItemsStartY = -1
	var renderItemSlotSize = -1

	var renderEquipmentStartX = -1
	var renderEquipmentStartY = -1
	var renderEquipmentSlotSize = -1
	var renderEquipmentSlotSpacing = -1
	var renderEquipmentCharacterSpacing = -1

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
		val party = state.characterSelection.party
		validatePartyIndex()

		if ((inside && key == InputKey.Interact) || key == InputKey.Click) {
			if (!inside) {
				inside = true
				soundQueue.insert("click-confirm")
			}

			if (hoveringItem != null) {
				var forbidden = false
				if (pickedUpItem != null && hoveringItem != pickedUpItem && hoveringItem!!.getEquipmentType() != null) {
					val pickedStack = pickedUpItem!!.get()!!
					if (pickedStack.amount > 1 || !hoveringItem!!.canInsert(pickedStack.item)) {
						forbidden = true
					}
				}
				if (pickedUpItem == null && hoveringItem!!.getEquipmentType() == EquipmentSlotType.MainHand) {
					forbidden = true
				}

				if (forbidden) {
					soundQueue.insert("click-reject")
				} else {
					val oldItem = hoveringItem!!.get()
					if (oldItem != null) {
						if (pickedUpItem == null) {
							pickedUpItem = hoveringItem
							soundQueue.insert("click-confirm")
						} else if (hoveringItem == pickedUpItem) {
							pickedUpItem = null
							soundQueue.insert("click-cancel")
						} else {
							if (oldItem.item == pickedUpItem!!.get()!!.item) {
								hoveringItem!!.set(ItemStack(oldItem.item, oldItem.amount + pickedUpItem!!.get()!!.amount))
								pickedUpItem!!.set(null)
								pickedUpItem = null
							} else {
								hoveringItem!!.set(pickedUpItem!!.get())
								pickedUpItem!!.set(oldItem)
							}
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
		}

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

		if (key == InputKey.MoveLeft) {
			descriptionIndex -= 1
			if (descriptionIndex == -1) descriptionIndex = 2
			soundQueue.insert("menu-scroll")
		}
		if (key == InputKey.MoveRight) {
			descriptionIndex += 1
			if (descriptionIndex == 3) descriptionIndex = 0
			soundQueue.insert("menu-scroll")
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
			if (slotX < 8 && slotY < 8) hoveringItem = ItemReference(
				assetCharacter, currentCharacter, slotX + 8 * slotY
			)
		}

		val party = state.characterSelection.party
		if (event.newX >= renderEquipmentStartX && event.newY >= renderEquipmentStartY) {
			val offsetX = event.newX - renderEquipmentStartX
			val offsetY = event.newY - renderEquipmentStartY
			val slotIndex = offsetX / renderEquipmentSlotSpacing
			val characterIndex = offsetY / renderEquipmentCharacterSpacing
			if (
				slotIndex < 6 && offsetX % renderEquipmentSlotSpacing < renderEquipmentSlotSize &&
				characterIndex < party.size && offsetY % renderEquipmentCharacterSpacing < renderEquipmentSlotSize
			) {
				val equipmentOwner = party[characterIndex]
				if (equipmentOwner != null) hoveringItem = ItemReference(
					equipmentOwner, state.characterStates[equipmentOwner]!!, -1 - slotIndex
				)
			}
		}

		mouseX = event.newX
		mouseY = event.newY
	}
}

class ItemReference(val assetCharacter: PlayableCharacter, val characterState: CharacterState, val slotIndex: Int) {

	override fun toString() = if (slotIndex >= 0) "inventory slot $slotIndex" else "equipment slot ${equipmentIndex()}"

	override fun equals(other: Any?) = other is ItemReference && assetCharacter === other.assetCharacter &&
			characterState === other.characterState && slotIndex == other.slotIndex

	override fun hashCode() = characterState.hashCode() + 31 * slotIndex

	private fun equipmentIndex() = -slotIndex - 1

	fun get() = if (slotIndex >= 0) characterState.inventory[slotIndex] else {
		val item = characterState.equipment[equipmentIndex()]
		if (item != null) ItemStack(item, 1) else null
	}

	fun set(newStack: ItemStack?) {
		if (slotIndex < 0) {
			if (newStack == null) characterState.equipment[equipmentIndex()] = null
			else if (newStack.amount == 1) characterState.equipment[equipmentIndex()] = newStack.item
			else throw IllegalArgumentException("Equipment can't stack: attempted to put $newStack")
		} else characterState.inventory[slotIndex] = newStack
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

	fun canInsert(candidate: Item): Boolean {
		val type = getEquipmentType() ?: return true
		val candidateEquipment = candidate.equipment ?: return false
		if (candidateEquipment.onlyUser != null && candidateEquipment.onlyUser != assetCharacter.name) return false
		if (type != candidateEquipment.getSlotType()) return false

		if (candidate == get()?.item) return false

		if (type == EquipmentSlotType.MainHand) {
			return assetCharacter.characterClass.weaponType == candidateEquipment.weapon?.type
		}

		if (type == EquipmentSlotType.Accessory) return true

		return assetCharacter.characterClass.armorTypes.contains(candidateEquipment.armorType)
	}
}
