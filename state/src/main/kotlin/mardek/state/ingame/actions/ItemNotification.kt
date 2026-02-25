package mardek.state.ingame.actions

import mardek.content.inventory.ItemStack
import kotlin.time.Duration

/**
 * A notification in the top-left of the screen when a player gains or loses an item due to
 * [mardek.content.action.ActionTakeItem] or [mardek.content.action.ActionGiveItem].
 */
class ItemNotification(

	/**
	 * The item and amount that were gained or lost
	 */
	val stack: ItemStack,

	/**
	 * Whether the player gained an item, or lost an item
	 */
	val operation: Operation,

	/**
	 * The time at which the `ActionTakeItem` or `ActionGiveItem` was performed. The difference between `timestamp`
	 * and the current time is needed for rendering: the notification should stop being rendered after a couple of
	 * seconds. Furthermore, it is needed for the fade-in & fade-out.
	 */
	val timestamp: Duration,
) {

	/**
	 * The operations of [ItemNotification], which are simply [Acquire] and [Lost].
	 */
	enum class Operation(
		/**
		 * The text that should be rendered before the item name (e.g. "Lost ")
		 */
		val prefix: String
	) {
		/**
		 * The player acquired the item
		 */
		Acquire(""),

		/**
		 * The player lost the item
		 */
		Lost("Lost "),
	}
}
