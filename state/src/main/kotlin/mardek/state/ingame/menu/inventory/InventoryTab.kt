package mardek.state.ingame.menu.inventory

import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.state.ingame.menu.InGameMenuTab
import mardek.state.ingame.menu.UiUpdateContext
import mardek.state.util.Rectangle

class InventoryTab: InGameMenuTab() {

	val interaction = InventoryInteractionState()
	var gridRenderInfo: ItemGridRenderInfo? = null
	var equipmentRenderInfo: Collection<EquipmentRowRenderInfo> = emptyList()
	var thrashRegion: Rectangle? = null
	var sortRegion: Rectangle? = null

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

			val hoveringItem = interaction.hoveredSlot
			if (hoveringItem != null) {
				val swapResult = hoveringItem.swap(context.getCursorStack(), context.sounds)
				if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
				context.setCursorStack(swapResult.newCursorStack)
			}

			thrashRegion?.let {
				if (it.contains(interaction.mouseX, interaction.mouseY)) {
					context.setCursorStack(null)
					context.soundQueue.insert(context.sounds.ui.clickCancel)
				}
			}

			for (info in equipmentRenderInfo) {
				if (info.consumableRegion.contains(interaction.mouseX, interaction.mouseY)) {
					val cursorStack = context.getCursorStack() ?: break
					val consumable = cursorStack.item.consumable
					if (consumable != null) {
						if (consumable.consumeOutsideBattle(info.owner, info.ownerState)) {
							context.setCursorStack(cursorStack.decremented())
							val soundEffect = consumable.particleEffect?.initialSound() ?: consumable.particleEffect?.damageSound()
							if (soundEffect != null) context.soundQueue.insert(soundEffect)
						} else context.soundQueue.insert(context.sounds.ui.clickReject)
					}
				}
			}
		}

		if (key == InputKey.SplitClick) {
			if (!inside) {
				inside = true
				context.soundQueue.insert(context.sounds.ui.clickConfirm)
			}
			interaction.hoveredSlot?.let {
				val swapResult = it.takeSingle(context.getCursorStack(), context.sounds)
				if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
				context.setCursorStack(swapResult.newCursorStack)
			}
		}

		val oldPartyIndex = interaction.partyIndex
		if (key == InputKey.MoveUp) {
			interaction.partyIndex -= 1
			while (interaction.partyIndex >= 0 && context.fullParty[interaction.partyIndex] == null) {
				interaction.partyIndex -= 1
			}
			if (interaction.partyIndex < 0) interaction.partyIndex = context.usedParty.last().index
			if (interaction.partyIndex != oldPartyIndex) {
				context.soundQueue.insert(context.sounds.ui.scroll2)
				processInteractionMouseMove(interaction.mouseX, interaction.mouseY, context)
			}
		}

		if (key == InputKey.MoveDown) {
			interaction.partyIndex += 1
			while (interaction.partyIndex < context.fullParty.size && context.fullParty[interaction.partyIndex] == null) {
				interaction.partyIndex += 1
			}
			if (interaction.partyIndex >= context.fullParty.size) interaction.partyIndex = context.usedParty[0].index
			if (interaction.partyIndex != oldPartyIndex) {
				context.soundQueue.insert(context.sounds.ui.scroll2)
				processInteractionMouseMove(interaction.mouseX, interaction.mouseY, context)
			}
		}

		interaction.processScroll(context.sounds, context.soundQueue, key)

		super.processKeyPress(key, context)
	}

	override fun processMouseMove(event: MouseMoveEvent, context: UiUpdateContext) {
		validatePartyIndex(context)
		if (gridRenderInfo == null) return
		processInteractionMouseMove(event.newX, event.newY, context)
	}

	private fun processInteractionMouseMove(newX: Int, newY: Int, context: UiUpdateContext) {
		val (_, currentCharacter) = context.fullParty[interaction.partyIndex]!!
		interaction.processMouseMove(
			newX, newY, gridRenderInfo!!,
			equipmentRenderInfo, currentCharacter,
		)
	}
}
