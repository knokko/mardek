package mardek.state.ingame.menu.inventory

import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.EquipmentSlotType
import mardek.content.inventory.Item
import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.content.characters.CharacterState
import mardek.content.inventory.ItemStack
import mardek.state.ingame.menu.InGameMenuTab
import mardek.state.ingame.menu.UiUpdateContext

class InventoryTab: InGameMenuTab() {

	val interaction = InventoryInteractionState()
	var gridRenderInfo: ItemGridRenderInfo? = null
	var equipmentRenderInfo: Collection<EquipmentRowRenderInfo> = emptyList()

	override fun getText() = "Inventory"

	override fun canGoInside() = true

	private fun validatePartyIndex(context: UiUpdateContext) {
		if (context.fullParty[interaction.partyIndex] == null) interaction.partyIndex = context.usedParty[0].index
	}

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		validatePartyIndex(context)

		if ((inside && key == InputKey.Interact) || key == InputKey.Click) {
			if (!inside) {
				inside = true
				context.soundQueue.insert(context.sounds.ui.clickConfirm)
			}

			interaction.processClick(context.sounds, context.soundQueue)
		}

		val oldPartyIndex = interaction.partyIndex
		if (key == InputKey.MoveUp) {
			interaction.partyIndex -= 1
			while (interaction.partyIndex >= 0 && context.fullParty[interaction.partyIndex] == null) {
				interaction.partyIndex -= 1
			}
			if (interaction.partyIndex < 0) interaction.partyIndex = context.usedParty.last().index
			if (interaction.partyIndex != oldPartyIndex) context.soundQueue.insert(context.sounds.ui.scroll2)
		}

		if (key == InputKey.MoveDown) {
			interaction.partyIndex += 1
			while (interaction.partyIndex < context.fullParty.size && context.fullParty[interaction.partyIndex] == null) {
				interaction.partyIndex += 1
			}
			if (interaction.partyIndex >= context.fullParty.size) interaction.partyIndex = context.usedParty[0].index
			if (interaction.partyIndex != oldPartyIndex) context.soundQueue.insert(context.sounds.ui.scroll2)
		}

		interaction.processScroll(context.sounds, context.soundQueue, key)

		super.processKeyPress(key, context)
	}

	override fun processMouseMove(event: MouseMoveEvent, context: UiUpdateContext) {
		validatePartyIndex(context)
		if (gridRenderInfo == null) return

		val (assetCharacter, currentCharacter) = context.fullParty[interaction.partyIndex]!!
		interaction.processMouseMove(
			event.newX, event.newY, gridRenderInfo!!, equipmentRenderInfo,
			assetCharacter, currentCharacter
		)
	}
}

class ItemReference(val owner: PlayableCharacter, val ownerState: CharacterState, val slotIndex: Int) {

	override fun toString() = if (slotIndex >= 0) "inventory slot $slotIndex" else "equipment slot ${equipmentIndex()}"

	override fun equals(other: Any?) = other is ItemReference && owner === other.owner &&
			ownerState === other.ownerState && slotIndex == other.slotIndex

	override fun hashCode() = ownerState.hashCode() + 31 * slotIndex

	private fun equipmentIndex() = -slotIndex - 1

	fun get() = if (slotIndex >= 0) ownerState.inventory[slotIndex] else {
		val item = ownerState.equipment[equipmentIndex()]
		if (item != null) ItemStack(item, 1) else null
	}

	fun set(newStack: ItemStack?) {
		if (slotIndex < 0) {
			if (newStack == null) ownerState.equipment[equipmentIndex()] = null
			else if (newStack.amount == 1) ownerState.equipment[equipmentIndex()] = newStack.item
			else throw IllegalArgumentException("Equipment can't stack: attempted to put $newStack")
		} else ownerState.inventory[slotIndex] = newStack
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
		if (candidateEquipment.onlyUser != null && candidateEquipment.onlyUser != owner.name) return false
		if (type != candidateEquipment.getSlotType()) return false

		if (candidate == get()?.item) return false

		if (type == EquipmentSlotType.MainHand) {
			return owner.characterClass.weaponType == candidateEquipment.weapon?.type
		}

		if (type == EquipmentSlotType.Accessory) return true

		return owner.characterClass.armorTypes.contains(candidateEquipment.armorType)
	}
}
