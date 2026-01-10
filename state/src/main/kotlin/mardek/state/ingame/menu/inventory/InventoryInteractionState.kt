package mardek.state.ingame.menu.inventory

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.EquipmentSlotType
import mardek.content.inventory.ItemStack
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
	 * The item that the user is currently picking up. This item should be rendered at `(mouseX, mouseY)` instead of
	 * being rendered inside its slot.
	 *
	 * Note that there is no 'slot' for picked-up items: when a user picks an item, that item will be **rendered** at
	 * the mouse position, but it will stay inside its slot until the user puts it in another slot. This is to avoid
	 * potential hazards where the inventory is closed while the player is holding an item.
	 */
	var pickedUpItem: ItemReference? = null

	/**
	 * The slot over which the mouse cursor is currently hovering. This slot should be rendered in a different color
	 * (blue) to indicate this.
	 */
	var hoveringItem: ItemReference? = null

	fun processClick(sounds: FixedSoundEffects, soundQueue: SoundQueue) {
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
				soundQueue.insert(sounds.ui.clickReject)
			} else {
				val oldItem = hoveringItem!!.get()
				if (oldItem != null) {
					if (pickedUpItem == null) {
						pickedUpItem = hoveringItem
						soundQueue.insert(sounds.ui.clickConfirm)
					} else if (hoveringItem == pickedUpItem) {
						pickedUpItem = null
						soundQueue.insert(sounds.ui.clickCancel)
					} else {
						if (oldItem.item == pickedUpItem!!.get()!!.item) {
							hoveringItem!!.set(ItemStack(oldItem.item, oldItem.amount + pickedUpItem!!.get()!!.amount))
							pickedUpItem!!.set(null)
							pickedUpItem = null
						} else {
							hoveringItem!!.set(pickedUpItem!!.get())
							pickedUpItem!!.set(oldItem)
						}
						soundQueue.insert(sounds.ui.clickConfirm)
					}
				} else if (pickedUpItem != null) {
					hoveringItem!!.set(pickedUpItem!!.get())
					pickedUpItem!!.set(null)
					pickedUpItem = null
					soundQueue.insert(sounds.ui.clickCancel)
				}
			}
		}
	}

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
		currentCharacter: PlayableCharacter, currentCharacterState: CharacterState,
	) {
		hoveringItem = null

		val gridSlotIndex = gridRenderInfo.determineSlotIndex(newX, newY)
		if (gridSlotIndex != -1) hoveringItem = ItemReference(
			currentCharacter, currentCharacterState, gridSlotIndex
		)

		for (renderInfo in equipmentRenderInfo) {
			val slotIndex = renderInfo.determineSlotIndex(newX, newY)
			if (slotIndex != 0) hoveringItem = ItemReference(renderInfo.owner, renderInfo.ownerState, slotIndex)
		}

		mouseX = newX
		mouseY = newY
	}
}
