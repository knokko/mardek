package mardek.state.ingame.menu.inventory

import mardek.content.audio.FixedSoundEffects
import mardek.content.audio.SoundEffect
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.EquipmentSlot
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack

/**
 * Represents a (reference to) a slot that contains an `Item` or `ItemStack`. This can be a simple inventory slot, but
 * can also be an equipment slot or item storage slot.
 */
sealed class ItemSlotReference {

	/**
	 * Gets the item (stack) that is currently present in this slot
	 */
	abstract fun get(): ItemStack?

	/**
	 * Attempts to swap the item stack in this slot with `cursorStack`.
	 *
	 * After calling this method, the `campaignState.cursorStack` should be replaced with `result.newCursorStack`.
	 * Furthermore, `result.sound` should be played if it's non-null.
	 */
	abstract fun swap(cursorStack: ItemStack?, sounds: FixedSoundEffects): ItemSwapResult

	/**
	 * Attempts to take a single item from the stack stored in this slot.
	 *
	 * After calling this method, the `campaignState.cursorStack` should be replaced with `result.newCursorStack`.
	 * Furthermore, `result.sound` should be played if it's non-null.
	 */
	abstract fun takeSingle(cursorStack: ItemStack?, sounds: FixedSoundEffects): ItemSwapResult
}

private fun simpleSwap(
	oldStack: ItemStack?, newStack: ItemStack?, sounds: FixedSoundEffects
): Pair<ItemStack?, ItemSwapResult> {
	return if (oldStack == null && newStack == null) {
		Pair(null, ItemSwapResult(null, null))
	} else if (oldStack == null) {
		Pair(newStack, ItemSwapResult(null, sounds.ui.clickCancel))
	} else if (newStack == null) {
		Pair(null, ItemSwapResult(oldStack, sounds.ui.clickConfirm))
	} else if (oldStack.item === newStack.item) {
		val returned = if (newStack.amount > 1) ItemSwapResult(
			newStack.decremented(), sounds.ui.
			clickConfirm) else ItemSwapResult(null, sounds.ui.clickCancel)
		Pair(oldStack.incremented(), returned)
	} else {
		Pair(newStack, ItemSwapResult(oldStack, sounds.ui.clickConfirm))
	}
}

private fun simpleTakeSimple(
	oldStack: ItemStack?, cursorStack: ItemStack?, sounds: FixedSoundEffects
): Pair<ItemStack?, ItemSwapResult> {
	return if (oldStack == null) {
		Pair(null, ItemSwapResult(cursorStack, null))
	} else if (cursorStack == null) {
		Pair(oldStack.decremented(), ItemSwapResult(
			ItemStack(oldStack.item, 1), sounds.ui.clickConfirm
		))
	} else if (oldStack.item === cursorStack.item) {
		Pair(oldStack.decremented(), ItemSwapResult(
			cursorStack.incremented(), sounds.ui.clickConfirm
		))
	} else {
		Pair(oldStack, ItemSwapResult(cursorStack, sounds.ui.clickReject))
	}
}

/**
 * A reference to an item slot inside a playable character inventory
 */
class InventorySlotReference(
	/**
	 * A reference to the corresponding `CharacterState.inventory`
	 */
	val inventory: Array<ItemStack?>,

	/**
	 * The index of the slot to which this reference refers: this reference points to `inventory[index]`.
	 */
	val index: Int,
) : ItemSlotReference() {
	override fun get() = inventory[index]

	override fun swap(cursorStack: ItemStack?, sounds: FixedSoundEffects): ItemSwapResult {
		val oldStack = inventory[index]
		val (newStack, result) = simpleSwap(oldStack, cursorStack, sounds)
		inventory[index] = newStack
		return result
	}

	override fun takeSingle(cursorStack: ItemStack?, sounds: FixedSoundEffects): ItemSwapResult {
		val oldStack = inventory[index]
		val (newStack, result) = simpleTakeSimple(oldStack, cursorStack, sounds)
		inventory[index] = newStack
		return result
	}

	override fun equals(other: Any?) = other is InventorySlotReference && this.inventory === other.inventory &&
			this.index == other.index

	override fun hashCode() = inventory.hashCode() + index
}

/**
 * A reference to an equipment slot of a playable character.
 */
class EquipmentSlotReference(
	/**
	 * The playable character that owns the equipment slot
	 */
	val owner: PlayableCharacter,

	/**
	 * The corresponding `CharacterState.equipment`
	 */
	val equipment: MutableMap<EquipmentSlot, Item>,

	/**
	 * The slot to which this reference refers: it refers to `equipment[slot]`
	 */
	val slot: EquipmentSlot,
) : ItemSlotReference() {

	override fun get() = equipment[slot]?.let { ItemStack(it, 1) }

	override fun swap(cursorStack: ItemStack?, sounds: FixedSoundEffects): ItemSwapResult {
		val oldItem = equipment[slot]
		return if (cursorStack == null) {
			if (oldItem == null) {
				ItemSwapResult(null, null)
			} else if (slot.canBeEmpty) {
				equipment.remove(slot)
				ItemSwapResult(ItemStack(oldItem, 1), sounds.ui.clickConfirm)
			} else {
				ItemSwapResult(null, sounds.ui.clickReject)
			}
		} else {
			if (slot.isAllowed(cursorStack.item, owner)) {
				if (oldItem == null) {
					equipment[slot] = cursorStack.item
					if (cursorStack.amount > 1) {
						ItemSwapResult(ItemStack(
							cursorStack.item, cursorStack.amount - 1
						), sounds.ui.clickCancel)
					} else {
						ItemSwapResult(null, sounds.ui.clickCancel)
					}
				} else if (oldItem === cursorStack.item) {
					ItemSwapResult(cursorStack, sounds.ui.clickReject)
				} else {
					if (cursorStack.amount == 1) {
						equipment[slot] = cursorStack.item
						ItemSwapResult(ItemStack(oldItem, 1), sounds.ui.clickConfirm)
					} else {
						ItemSwapResult(cursorStack, sounds.ui.clickReject)
					}
				}
			} else {
				ItemSwapResult(cursorStack, sounds.ui.clickReject)
			}
		}
	}

	override fun takeSingle(
		cursorStack: ItemStack?,
		sounds: FixedSoundEffects
	): ItemSwapResult {
		val oldItem = equipment[slot] ?: return ItemSwapResult(cursorStack, null)
		if (!slot.canBeEmpty) return ItemSwapResult(cursorStack, sounds.ui.clickReject)
		if (cursorStack == null) {
			equipment.remove(slot)
			return ItemSwapResult(ItemStack(oldItem, 1), sounds.ui.clickConfirm)
		}
		return if (cursorStack.item === oldItem) {
			equipment.remove(slot)
			ItemSwapResult(cursorStack.incremented(), sounds.ui.clickConfirm)
		} else ItemSwapResult(cursorStack, sounds.ui.clickReject)
	}
}

/**
 * A reference to a slot in the item storage.
 */
class ItemStorageSlotReference(
	/**
	 * A reference to the `campaignState.itemStorage`
	 */
	val storage: MutableList<ItemStack?>,

	/**
	 * The index of the slot into the item storage: this reference points to `storage[index]`
	 */
	val index: Int,
) : ItemSlotReference() {

	override fun get() = if (index < storage.size) storage[index] else null

	override fun swap(
		cursorStack: ItemStack?,
		sounds: FixedSoundEffects
	): ItemSwapResult {
		while (index >= storage.size) storage.add(null)
		val oldStack = storage[index]
		val (newStack, result) = simpleSwap(oldStack, cursorStack, sounds)
		storage[index] = newStack
		return result
	}

	override fun takeSingle(
		cursorStack: ItemStack?,
		sounds: FixedSoundEffects
	): ItemSwapResult {
		while (index >= storage.size) storage.add(null)
		val oldStack = storage[index]
		val (newStack, result) = simpleTakeSimple(oldStack, cursorStack, sounds)
		storage[index] = newStack
		return result
	}
}

/**
 * This class is used as return type for [ItemSlotReference.swap] and [ItemSlotReference.takeSingle]. It is just a
 * simple tuple `(newCursorStack, sound)`, which is nicer than just returning a `Pair<ItemStack?, SoundEffect?>`
 */
class ItemSwapResult(
	/**
	 * The `cursorItemStack` of the `CampaignState` should be set to `this.newCursorStack`
	 */
	val newCursorStack: ItemStack?,

	/**
	 * If this sound effect is non-null, it should be played
	 */
	val sound: SoundEffect?,
)
