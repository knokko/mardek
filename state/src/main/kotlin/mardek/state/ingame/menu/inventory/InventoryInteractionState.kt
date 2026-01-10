package mardek.state.ingame.menu.inventory

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.CharacterState
import mardek.input.InputKey
import mardek.state.SoundQueue

/**
 * This class contains the part of the inventory/item-storage interaction state that is relevant for the renderer.
 * This information is propagated from the state to the renderer.
 */
class InventoryInteractionState {
	/**
	 * The X-coordinate of the mouse cursor, relative to the left window border
	 */
	var mouseX = -1

	/**
	 * The Y-coordinate of the mouse cursor, relative to the top window border. Note that moving the mouse down
	 * *in*creases `mouseY`.
	 */
	var mouseY = -1

	/**
	 * The index of the currently-selected party member (whose inventory should be rendered)
	 */
	var partyIndex = 0

	/**
	 * The 'index' of the type of item description that should be shown:
	 * - 0 means Description
	 * - 1 means Skills
	 * - 2 means Properties
	 */
	var descriptionIndex = 0

	/**
	 * The slot over which the mouse cursor is currently hovering. This slot should be rendered in a different color
	 * (blue) to indicate this.
	 */
	var hoveredSlot: ItemSlotReference? = null

	fun processScroll(sounds: FixedSoundEffects, soundQueue: SoundQueue, key: InputKey) {
		if (key == InputKey.MoveLeft) {
			descriptionIndex -= 1
			if (descriptionIndex == -1) descriptionIndex = 2
			soundQueue.insert(sounds.ui.scroll1)
		}
		if (key == InputKey.MoveRight) {
			descriptionIndex += 1
			if (descriptionIndex == 3) descriptionIndex = 0
			soundQueue.insert(sounds.ui.scroll1)
		}
	}

	fun processMouseMove(
		newX: Int, newY: Int, gridRenderInfo: ItemGridRenderInfo,
		equipmentRenderInfo: Collection<EquipmentRowRenderInfo>,
		currentCharacterState: CharacterState,
	) {
		hoveredSlot = null

		val gridSlotIndex = gridRenderInfo.determineSlotIndex(newX, newY)
		if (gridSlotIndex != -1) hoveredSlot = InventorySlotReference(currentCharacterState.inventory, gridSlotIndex)

		for (renderInfo in equipmentRenderInfo) {
			val slotIndex = renderInfo.determineSlotIndex(newX, newY)
			if (slotIndex != -1) {
				val slot = renderInfo.owner.characterClass.equipmentSlots[slotIndex]
				hoveredSlot = EquipmentSlotReference(renderInfo.owner, renderInfo.ownerState.equipment, slot)
			}
		}

		mouseX = newX
		mouseY = newY
	}
}
