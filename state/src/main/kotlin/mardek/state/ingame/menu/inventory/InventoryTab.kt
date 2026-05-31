package mardek.state.ingame.menu.inventory

import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.state.ingame.menu.InGameMenuTab
import mardek.state.ingame.menu.UiUpdateContext
import mardek.state.util.Rectangle

/**
 * The "Inventory" tab of the in-game menu.
 *
 * This class tracks the state of the player interaction with the inventory. It tracks e.g. over which slot the mouse
 * cursor is hovering, and where the inventory grid and equipment bars are rendered.
 *
 * This class does *not* capture the actual inventory states,
 * which are part of the [mardek.content.characters.CharacterState]s.
 */
class InventoryTab: InGameMenuTab() {

	/**
	 * The 'interaction state'. This tracks e.g. over which slot the mouse cursor is hovering.
	 */
	val interaction = InventoryInteractionState()

	/**
	 * The position where the (8x8) inventory grid was rendered, or `null` if the first frame hasn't been rendered yet.
	 */
	var gridRenderInfo: ItemGridRenderInfo? = null

	/**
	 * For each party member whose equipment/character bar was rendered, this list tracks in which position it was
	 * rendered. Note that it will be empty until the first frame is rendered.
	 */
	var equipmentRenderInfo: Collection<EquipmentRowRenderInfo> = emptyList()

	/**
	 * The region where the thrash icon (to discard items) was rendered, or `null` before the first frame.
	 */
	var thrashRegion: Rectangle? = null

	/**
	 * The region where the sort icon was rendered, or `null` before it has been rendered.
	 * TODO CHAP2 Actually render this...
	 */
	var sortRegion: Rectangle? = null

	private var wentInsideByClicking = false

	override fun getText() = "Inventory"

	override fun canGoInside() = true

	private fun validatePartyIndex(context: UiUpdateContext) {
		if (context.fullParty[interaction.partyIndex] == null) interaction.partyIndex = context.usedParty[0].index
	}

	override fun processKeyPress(key: InputKey, context: UiUpdateContext) {
		validatePartyIndex(context)

		if (!inside) wentInsideByClicking = false

		if ((inside && key == InputKey.Interact) || key == InputKey.Click) {

			val hoveringItem = interaction.hoveredSlot
			if (hoveringItem != null) {
				val swapResult = hoveringItem.swap(context.getCursorStack(), context.sounds)
				if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
				context.setCursorStack(swapResult.newCursorStack)

				if (swapResult.newCursorStack != null && !inside) {
					inside = true
					wentInsideByClicking = true
				}
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
							context.statistics.itemsConsumed += 1
						} else context.soundQueue.insert(context.sounds.ui.clickReject)
					}
				}
			}
		}

		if (key == InputKey.SplitClick) {
			interaction.hoveredSlot?.let {
				val swapResult = it.takeSingle(context.getCursorStack(), context.sounds)
				if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
				context.setCursorStack(swapResult.newCursorStack)

				if (swapResult.newCursorStack != null && !inside) {
					inside = true
					wentInsideByClicking = true
				}
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

		if (inside && wentInsideByClicking && context.getCursorStack() == null) inside = false
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
