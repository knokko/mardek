package mardek.state.ingame.menu.inventory

/**
 * This class is used to propagate information about the rendered item grid of an inventory from the renderer to the
 * state. The state needs to know where the item grid was rendered, to determine whether the user clicks on the item
 * grid.
 *
 * Each slot `(columnX, rowY)` is rendered between the coordinates
 * `(startX + columnX * slotSize, startY + rowY * slotSize)` and
 * `(startX + (columnX + 1) * slotSize - 1, startY + (rowY + 1) * slotSize - 1)`.
 */
class ItemGridRenderInfo(
	/**
	 * The X-coordinate of the left-most coordinate where the item grid was rendered
	 */
	val startX: Int,

	/**
	 * The Y-coordinate of the up-most coordinate where the item grid was rendered
	 */
	val startY: Int,

	/**
	 * The slot size (width = height) of each inventory slot, including the border of the slot.
	 */
	val slotSize: Int
) {

	/**
	 * If the coordinates `(x, y)` are inside a rendered slot, the index of that item slot is returned. Otherwise -1
	 * is returned.
	 */
	fun determineSlotIndex(x: Int, y: Int): Int {
		if (x >= startX && y >= startY && slotSize > 0) {
			val slotX = (x - startX) / slotSize
			val slotY = (y - startY) / slotSize
			if (slotX < 8 && slotY < 8) return slotX + 8 * slotY
		}
		return -1
	}
}
