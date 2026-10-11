package mardek.content.util

import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack

/**
 * Creates a new sorted list of [ItemStack]s containing all items in [originalInventory].
 *
 * The new list will contain every [Item] at most once. If the original inventory contains e.g. a stack of 2 potions and
 * another stack of 3 potions, the new list will contain one stack of 5 potions.
 */
fun getSortedInventory(originalInventory: Array<ItemStack?>): List<ItemStack> {
	val rawGroupedItems = originalInventory.filterNotNull().groupBy { it.item }
	val groupedItems = rawGroupedItems.values.map { it.reduce {
			left, right -> ItemStack(left.item, left.amount + right.amount) }
	}
	return groupedItems.sortedBy { it.item.sortIndex }
}
