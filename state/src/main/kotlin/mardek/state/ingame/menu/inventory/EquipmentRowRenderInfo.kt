package mardek.state.ingame.menu.inventory

import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.state.util.Rectangle

/**
 * This class is used to propagate information about the rendered equipment of a playable character in an inventory,
 * from the renderer to the state. The state needs to know where the equipped items were rendered,
 * to determine whether the user clicks on equipped items or slots.
 *
 * Each slot `equipmentIndex` is rendered between `(startX + slotSpacing * equipmentIndex, startY)` and
 * `(startX + slotSpacing * (1 + equipmentIndex) - 1, startY + slotSize -1)`.
 */
class EquipmentRowRenderInfo(
	/**
	 * The X-coordinate of the left-most point where the first equipped item slot (the weapon slot) is rendered.
	 */
	val startX: Int,

	/**
	 * The Y-coordinate of the up-most point where each equipment slot is rendered.
	 */
	val startY: Int,

	/**
	 * The slot size (width = height) of each equipment slot, including the border of the slot.
	 */
	val slotSize: Int,

	/**
	 * The horizontal spacing/distance between each consecutive equipment slot.
	 */
	val slotSpacing: Int,

	/**
	 * The number of equipment slots that were rendered.
	 */
	val numSlots: Int,

	/**
	 * The region where the player can feed consumable items (e.g. potions) to `owner`
	 */
	val consumableRegion: Rectangle,

	/**
	 * The character whose equipment was rendered in this row
	 */
	val owner: PlayableCharacter,

	/**
	 * The character state of `owner`
	 */
	val ownerState: CharacterState,
) {

	/**
	 * If the coordinates `(x, y)` are inside a rendered equipment slot, the index of that equipment slot is returned.
	 * Otherwise, -1 is returned.
	 */
	fun determineSlotIndex(x: Int, y: Int): Int {
		if (x >= startX && y >= startY && slotSize > 0 && slotSpacing > 0) {
			val offsetX = x - startX
			val offsetY = y - startY
			val slotIndex = offsetX / slotSpacing
			if (slotIndex < numSlots && offsetX % slotSpacing < slotSize && offsetY < slotSize) {
				return slotIndex
			}
		}
		return -1
	}
}
