package mardek.state.ingame.actions

import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.ItemStack
import mardek.input.InputKey
import mardek.state.ingame.menu.inventory.EquipmentRowRenderInfo
import mardek.state.ingame.menu.inventory.InventoryInteractionState
import mardek.state.ingame.menu.inventory.ItemGridRenderInfo
import mardek.state.ingame.menu.inventory.ItemStorageSlotReference
import mardek.state.util.Rectangle

/**
 * When the player is using the item storage, this class tracks the state of the item storage interaction
 * (e.g. which character inventory is being viewed, which slot is being hovered, etc...).
 *
 * This is stored in [AreaActionsState.itemStorageInteraction].
 */
class ItemStorageInteractionState {

	/**
	 * The interaction state with the inventory of the selected character.
	 *
	 * The [InventoryInteractionState.mouseX] and `mouseY` are also (ab)used when no character has been selected yet.
	 */
	val inventory = InventoryInteractionState()

	/**
	 * The currently-selected playable character, and its state
	 */
	var selectedCharacter: Pair<PlayableCharacter, CharacterState>? = null

	/**
	 * When the player is hovering their mouse cursor over a 'playable character slot', this field tells which
	 * character is being hovered.
	 *
	 * When the player is *not* hovering over a character slot, this field is `null`.
	 */
	var hoveredCharacter: PlayableCharacter? = null

	/**
	 * The 'page' of the item storage inventory that is currently being shown. The first page is 0, the second page
	 * is 1, etc...
	 *
	 * The pages in this engine are *not* identical to the ones in vanilla MARDEK. In this engine, the number of item
	 * storage pages is unlimited, but the player can only scroll to the next page when the last page contains at least
	 * 1 item.
	 *
	 * Note that the page size depends on the screen resolution: the engine will attempt to render as many item storage
	 * slots as possible, within a set of rendering constraints.
	 */
	var storagePage = 0

	/**
	 * The positions of all the rendered 'character item slot's
	 */
	var renderedCharacters: Array<ItemStorageCharacter> = emptyArray()

	/**
	 * The rendering position of the character/equipment bar, or `null` before the first rendering frame
	 */
	var renderedCharacterBar: EquipmentRowRenderInfo? = null

	/**
	 * The rendering position of the inventory of the selected playable character, or `null` if no frame with a
	 * selected character has been rendered yet
	 */
	var renderedCharacterInventory: ItemGridRenderInfo? = null

	/**
	 * The rendering position of the item storage inventory, or `null` before the first rendering frame
	 */
	var renderedStorageInventory: ItemStorageRenderInfo? = null

	/**
	 * The rendering position of the thrash/discard item icon/button
	 */
	var thrashRegion: Rectangle? = null

	/**
	 * Whether the player is allowed to scroll to the next item storage page. Players can only scroll through the next
	 * page if the current page or any later page contains at least 1 item.
	 */
	fun canScrollToNextPage(storage: List<ItemStack?>): Boolean {
		val rendered = renderedStorageInventory
		return if (rendered != null) {
			val pageSize = rendered.numRows * rendered.numColumns
			val lastIndex = storage.indexOfLast { it != null }
			if (lastIndex < 0) return false
			val lastPage = lastIndex / pageSize
			storagePage <= lastPage
		} else false
	}

	fun processKeyPress(context: AreaActionsState.UpdateContext, key: InputKey) {
		inventory.processScroll(context.sounds, context.soundQueue, key)
		if (key == InputKey.MoveUp && storagePage > 0) {
			storagePage -= 1
			context.soundQueue.insert(context.sounds.ui.scroll2)
		}
		if (key == InputKey.MoveDown && canScrollToNextPage(context.itemStorage)) {
			storagePage += 1
			context.soundQueue.insert(context.sounds.ui.scroll2)
		}

		if (key == InputKey.Click) {
			inventory.hoveredSlot?.let {
				val swapResult = it.swap(context.getCursorStack(), context.sounds)
				if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
				context.setCursorStack(swapResult.newCursorStack)
			}

			if (hoveredCharacter != null) {
				selectedCharacter = Pair(
					hoveredCharacter!!, context.playableCharacterStates[hoveredCharacter!!]!!
				)
			}

			thrashRegion?.let {
				if (it.contains(inventory.mouseX, inventory.mouseY)) {
					context.setCursorStack(null)
					context.soundQueue.insert(context.sounds.ui.clickCancel)
				}
			}
		}

		if (key == InputKey.SplitClick) {
			inventory.hoveredSlot?.let {
				val swapResult = it.takeSingle(context.getCursorStack(), context.sounds)
				if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
				context.setCursorStack(swapResult.newCursorStack)
			}
		}
	}

	fun processMouseMove(context: AreaActionsState.UpdateContext, newX: Int, newY: Int) {
		inventory.processMouseMove(
			newX, newY, renderedCharacterInventory,
			if (renderedCharacterBar != null) listOf(renderedCharacterBar!!) else emptyList(),
			selectedCharacter?.second,
		)

		hoveredCharacter = null
		for (potentialCharacter in renderedCharacters) {
			if (potentialCharacter.region.contains(newX, newY)) hoveredCharacter = potentialCharacter.character
		}

		updateHoveredStorageSlot(context.itemStorage)
	}

	/**
	 * Checks whether the player is currently hovering over an item storage inventory slot. If so,
	 * `inventory.hoveredSlot` is updated. This method must be invoked by the rendering after updating
	 * [renderedStorageInventory].
	 */
	fun updateHoveredStorageSlot(itemStorage: MutableList<ItemStack?>) {
		renderedStorageInventory?.let {
			if (inventory.mouseX >= it.startX && inventory.mouseY >= it.startY) {
				val column = (inventory.mouseX - it.startX) / it.slotSize
				val row = (inventory.mouseY - it.startY) / it.slotSize
				if (column < it.numColumns && row < it.numRows) {
					val storageIndex = it.startIndex + row * it.numColumns + column
					inventory.hoveredSlot = ItemStorageSlotReference(itemStorage, storageIndex)
				}
			}
		}
	}
}

/**
 * Represents a rendered 'playable character item slot'. It tells which character was rendered within which rectangle.
 */
class ItemStorageCharacter(
	/**
	 * The playable character that was rendered in this slot, or `null` if this slot is empty
	 */
	val character: PlayableCharacter?,

	/**
	 * The rendered region (on the window) that is occupied by this character slot
	 */
	val region: Rectangle,
)

/**
 * Represents information about where the item storage inventory was rendered on the window/screen
 */
class ItemStorageRenderInfo(

	/**
	 * The index (into [mardek.state.ingame.CampaignState.itemStorage] of the first slot that was rendered
	 */
	val startIndex: Int,

	/**
	 * The left-most X-coordinate where the first slot was rendered
	 */
	val startX: Int,

	/**
	 * The top-most Y-coordinate where the first slot was rendered
	 */
	val startY: Int,

	/**
	 * The size and spacing of each slot, in pixels
	 */
	val slotSize: Int,

	/**
	 * The number of item slot rows that was rendered
	 */
	val numRows: Int,

	/**
	 * The number of item slot columns that was rendered
	 */
	val numColumns: Int,
)
