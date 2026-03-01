package mardek.state.ingame.actions

import mardek.content.area.AreaShop
import mardek.content.inventory.Item
import mardek.content.inventory.ItemStack
import mardek.input.InputKey
import mardek.state.ingame.CampaignState
import mardek.state.ingame.area.ShopState
import mardek.state.ingame.menu.inventory.EquipmentRowRenderInfo
import mardek.state.ingame.menu.inventory.InventoryInteractionState
import mardek.state.ingame.menu.inventory.ItemGridRenderInfo
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.min

/**
 * When the player is interacting with a shop, this class tracks the state of the shop interaction
 * (e.g. which character inventory is being viewed, which slot is being hovered, etc...).
 *
 * This is stored in [AreaActionsState.shopInteraction].
 */
class ShopInteractionState(

	/**
	 * The shop where the player is browsing/trading items
	 */
	val shop: AreaShop,
) {

	/**
	 * The interaction state with the inventory of the selected character.
	 */
	val inventory = InventoryInteractionState()

	/**
	 * The rendering position of the character/equipment bar of each party member,
	 * or an empty array before the first rendering frame
	 */
	var renderedCharacterBars: Array<EquipmentRowRenderInfo> = emptyArray()

	/**
	 * The rendering position of the inventory of the selected playable character, or `null` if no frame with a
	 * selected character has been rendered yet
	 */
	var renderedCharacterInventory: ItemGridRenderInfo? = null

	/**
	 * The rendering position of the shop inventory, or `null` before the first rendering frame
	 */
	var renderedShopInventory: ItemGridRenderInfo? = null

	/**
	 * The currently-pending trade action (buying or sell), or `null` if no item stack is currently selected to be
	 * bought or sold.
	 */
	var pendingTrade: PendingTrade? = null

	/**
	 * The rendering position of the thrash/discard item icon/button
	 */
	var thrashRegion: Rectangle? = null

	/**
	 * The index of the shop inventory slot over which the mouse cursor is hovering, or -1 if the mouse cursor is not
	 * hovering over any shop inventory slot.
	 */
	var hoveredShopInventoryIndex = -1

	private var playedOpeningSound = false

	private fun validatePartyIndex(context: AreaActionsState.UpdateContext) {
		if (context.campaign.allPartyMembers()[inventory.partyIndex] == null) {
			inventory.partyIndex = context.campaign.usedPartyMembers()[0].index
		}
	}

	/**
	 * This method should be invoked during [AreaActionsState.processKeyEvent] whenever a key is pressed while
	 * a shop action is active.
	 */
	internal fun processKeyPress(context: AreaActionsState.UpdateContext, key: InputKey): Boolean {
		validatePartyIndex(context)

		val sounds = context.content.audio.fixedEffects
		val pendingTrade = this.pendingTrade
		if (pendingTrade == null) {
			inventory.processScroll(sounds, context.soundQueue, key)

			if (key == InputKey.Click) {
				inventory.hoveredSlot?.let {
					val swapResult = it.swap(context.campaign.cursorItemStack, sounds)
					if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
					context.campaign.cursorItemStack = swapResult.newCursorStack
				}

				if (hoveredShopInventoryIndex != -1 && inventory.hoveredSlot == null) {
					this.pendingTrade = PendingTrade.initiate(
						context.campaign.gold, context.campaign.cursorItemStack,
						hoveredShopInventoryIndex, shop, context.campaign.shops.get(shop),
					)
					context.soundQueue.insert(
						if (this.pendingTrade == null) sounds.ui.clickReject else sounds.ui.clickConfirm
					)
				}

				thrashRegion?.let {
					if (it.contains(inventory.mouseX, inventory.mouseY)) {
						context.campaign.cursorItemStack = null
						context.soundQueue.insert(sounds.ui.clickCancel)
					}
				}
			}

			if (key == InputKey.SplitClick) {
				inventory.hoveredSlot?.let {
					val swapResult = it.takeSingle(context.campaign.cursorItemStack, sounds)
					if (swapResult.sound != null) context.soundQueue.insert(swapResult.sound)
					context.campaign.cursorItemStack = swapResult.newCursorStack
				}
			}

			if (key == InputKey.MoveUp || key == InputKey.MoveDown) {
				val partyMembers = context.campaign.usedPartyMembers()
				val oldIndex = inventory.partyIndex

				if (key == InputKey.MoveUp) {
					inventory.partyIndex = (partyMembers.lastOrNull {
						it.index < inventory.partyIndex
					} ?: partyMembers.last()).index
				} else {
					inventory.partyIndex = (partyMembers.firstOrNull {
						it.index > inventory.partyIndex
					} ?: partyMembers.first()).index
				}

				if (inventory.partyIndex != oldIndex) {
					context.soundQueue.insert(sounds.ui.scroll2)
					inventory.processMouseMove(
						inventory.mouseX, inventory.mouseY,
						renderedCharacterInventory!!,
						renderedCharacterBars.toList(),
						partyMembers.find { it.index == inventory.partyIndex }!!.state,
					)
				}
			}

			if (key == InputKey.Cancel || key == InputKey.Escape) return true
		} else {
			if (key == InputKey.MoveLeft && pendingTrade.amount > 1) pendingTrade.amount -= 1
			if (key == InputKey.MoveDown) pendingTrade.amount = max(1, pendingTrade.amount - 10)

			val oldCursorStack = context.campaign.cursorItemStack
			val shopState = context.campaign.shops.get(shop)
			val maxAmount = pendingTrade.getMaxAmount(context.campaign.gold, oldCursorStack, shopState)

			if (key == InputKey.MoveRight) {
				pendingTrade.amount = min(1 + pendingTrade.amount, maxAmount)
			}

			if (key == InputKey.MoveUp) {
				pendingTrade.amount = min(10 + pendingTrade.amount, maxAmount)
			}

			if (key == InputKey.Interact || key == InputKey.ToggleMenu) {
				pendingTrade.execute(context.campaign, shop)
				this.pendingTrade = null
				context.soundQueue.insert(sounds.ui.trade)
			}

			if (key == InputKey.Cancel || key == InputKey.Escape) {
				this.pendingTrade = null
				context.soundQueue.insert(sounds.ui.clickCancel)
			}
		}

		return false
	}

	/**
	 * This method should be invoked during [AreaActionsState.processMouseMove] when the current action is a shop
	 * action.
	 */
	internal fun processMouseMove(context: AreaActionsState.UpdateContext, newX: Int, newY: Int) {
		validatePartyIndex(context)
		if (pendingTrade != null) return

		inventory.processMouseMove(
			newX, newY, renderedCharacterInventory,
			renderedCharacterBars.toList(),
			context.campaign.allPartyMembers()[inventory.partyIndex]!!.second,
		)
		updateHoveredShopSlot()
	}

	/**
	 * Checks whether the mouse cursor is currently hovering over an item slot of the shop inventory, and updates
	 * [hoveredShopInventoryIndex] accordingly.
	 */
	fun updateHoveredShopSlot() {
		hoveredShopInventoryIndex = renderedShopInventory?.determineSlotIndex(
			inventory.mouseX, inventory.mouseY
		) ?: -1
	}

	/**
	 * This method should be invoked during every [AreaActionsState.update] while the current action is a shop action.
	 */
	internal fun update(context: AreaActionsState.UpdateContext) {
		validatePartyIndex(context)
		if (!playedOpeningSound) {
			context.soundQueue.insert(context.content.audio.fixedEffects.ui.openMenu)
			playedOpeningSound = true
		}
	}
}

/**
 * Use this 'magic' buy/sell amount cap to avoid approaching `Integer.MAX_VALUE` anywhere
 */
private const val AMOUNT_CAP = 99999

private fun baseBuyAmountCap(gold: Int, item: Item) = if (item.cost > 0) gold / item.cost else AMOUNT_CAP

/**
 * This is the type of [ShopInteractionState.pendingTrade]. It represents either a potential buy action, or a
 * potential sell action.
 */
sealed class PendingTrade {

	/**
	 * The amount of items that the player wants to buy or sell. This is initially 1, but the player can increase or
	 * decrease it by pressing the arrow keys or AWSD.
	 *
	 * This value must always be at least 1, and must never be larger than the result of [getMaxAmount].
	 * (So it is forbidden to create an instance of [PendingTrade] where [getMaxAmount] would return 0.)
	 */
	var amount = 1

	/**
	 * Gets the item that the player is considering to buy or sell
	 */
	abstract fun item(cursorStack: ItemStack?, shopState: ShopState): Item

	/**
	 * Determines the maximum value that [amount] can get. If the player keeps pressing the Up Arrow or Right Arrow
	 * key, the [amount] will get 'stuck' at this value.
	 *
	 * This maximum amount depends on how much money the player has, and how much stock the shop has (e.g. the player
	 * can't buy 10 elixirs if the shop only has 1 in stock, or if the player only has the gold to buy 2).
	 */
	internal abstract fun getMaxAmount(gold: Int, cursorStack: ItemStack?, shopState: ShopState): Int

	/**
	 * Confirms/executes the trade: this will modify the cursor item stack, the player gold, and possibly the shop
	 * inventory.
	 */
	internal abstract fun execute(campaign: CampaignState, shop: AreaShop)

	companion object {

		/**
		 * Initiates a trade with the shop, if possible. If a trade is possible, a [PendingTrade] with `amount=1`
		 * will be returned. If no trade is possible, this method returns `null`.
		 *
		 * To demonstrate the possible cases, let `cursorStack` be the item stack on the cursor, and let `shopStack`
		 * be the item stack in the shop inventory slot over which the cursor is hovering.
		 * - If both `cursorStack` and `shopStack` are null/empty, no trade is possible, so this returns `null`
		 * - If `cursorStack` is empty, but `shopStack` is not, the player can buy an item, at least if he has enough
		 * money. In this case, this method would return an instance of [PendingBuyItem] or [PendingBuyStack].
		 * (But, if the player doesn't have enough money, it returns `null` instead.)
		 * - If `cursorStack` is non-empty, but `shopStack` is empty, this returns an instance of [PendingSell].
		 * - If both `cursorStack` and `shopStack` are non-empty, a trade is only possible if both are item stacks with
		 * the same item, otherwise this method returns `null`. When the items are the same, both a buy action and a
		 * sell action are possible in theory, but this game will choose a [PendingBuyStack] action, since that is
		 * probably more useful. (Unless the player doesn't have enough money, in which case it returns `null`...)
		 */
		fun initiate(
			gold: Int, cursorStack: ItemStack?,
			hoveredShopInventoryIndex: Int,
			shop: AreaShop, shopState: ShopState,
		) : PendingTrade? {
			val shopItem = shop.fixedItems[hoveredShopInventoryIndex]
			return if (shopItem != null) {
				val canAfford1 = shopItem.cost <= gold
				val canCursorTake = cursorStack == null || cursorStack.item === shopItem
				if (canAfford1 && canCursorTake) {
					PendingBuyItem(shopItem)
				} else null
			} else {
				val shopStack = shopState.inventory[hoveredShopInventoryIndex]
				if (shopStack != null) {
					val canAfford1 = shopStack.item.cost <= gold
					val canCursorTake = cursorStack == null || cursorStack.item === shopStack.item
					if (canAfford1 && canCursorTake) {
						PendingBuyStack(hoveredShopInventoryIndex)
					} else null
				} else if (cursorStack != null ){
					PendingSell(hoveredShopInventoryIndex)
				} else null
			}
		}
	}
}

/**
 * The player is considering to buy an item of which the shop has an endless supply. (e.g. item shops often have an
 * endless supply of potions and antidotes)
 */
class PendingBuyItem(

	/**
	 * The item that the player is considering to buy
	 */
	val item: Item
) : PendingTrade() {

	override fun item(
		cursorStack: ItemStack?,
		shopState: ShopState
	) = item

	override fun getMaxAmount(
		gold: Int,
		cursorStack: ItemStack?,
		shopState: ShopState
	) = baseBuyAmountCap(gold, item)

	override fun execute(campaign: CampaignState, shop: AreaShop) {
		campaign.gold -= amount * item.cost
		campaign.cursorItemStack = ItemStack(
			item, (campaign.cursorItemStack?.amount ?: 0) + amount
		)
	}

	override fun equals(other: Any?) = other is PendingBuyItem && this.amount == other.amount &&
			this.item === other.item

	override fun hashCode() = amount - 23 * item.id.hashCode()
}

/**
 * The player is considering to buy an item of which the shop has a limited supply. (e.g. some shops can only sell
 * certain items once or twice)
 */
class PendingBuyStack(

	/**
	 * The index of the shop inventory slot from which the player is considering to buy the item.
	 */
	val shopInventoryIndex: Int
) : PendingTrade() {

	override fun item(
		cursorStack: ItemStack?,
		shopState: ShopState
	) = shopState.inventory[shopInventoryIndex]!!.item

	override fun getMaxAmount(
		gold: Int,
		cursorStack: ItemStack?,
		shopState: ShopState
	) = min(
		shopState.inventory[shopInventoryIndex]!!.amount,
		baseBuyAmountCap(gold, shopState.inventory[shopInventoryIndex]!!.item),
	)

	override fun execute(campaign: CampaignState, shop: AreaShop) {
		val shopState = campaign.shops.get(shop)
		val item = item(campaign.cursorItemStack, shopState)
		campaign.gold -= amount * item.cost
		campaign.cursorItemStack = ItemStack(
			item, (campaign.cursorItemStack?.amount ?: 0) + amount
		)
		val oldShopStack = shopState.inventory[shopInventoryIndex]!!
		val newShopStack = if (oldShopStack.amount > amount) {
			ItemStack(item, oldShopStack.amount - amount)
		} else null
		shopState.inventory[shopInventoryIndex] = newShopStack
	}

	override fun equals(other: Any?) = other is PendingBuyStack && this.amount == other.amount &&
			this.shopInventoryIndex == other.shopInventoryIndex

	override fun hashCode() = amount - 13 * shopInventoryIndex
}

/**
 * The player is considering to sell the item stack that is currently on their cursor.
 */
class PendingSell(

	/**
	 * The index of the shop inventory slot to which the player is considering to sell their cursor item stack. If the
	 * player decides to sell, the cursor item stack will be stored in this shop inventory slot, from which the player
	 * could also buy it back later.
	 */
	val shopInventoryIndex: Int
) : PendingTrade() {

	override fun item(
		cursorStack: ItemStack?,
		shopState: ShopState
	) = cursorStack!!.item

	override fun getMaxAmount(
		gold: Int,
		cursorStack: ItemStack?,
		shopState: ShopState
	) = cursorStack!!.amount

	override fun execute(campaign: CampaignState, shop: AreaShop) {
		val shopState = campaign.shops.get(shop)
		val oldCursorStack = campaign.cursorItemStack!!
		campaign.gold += amount * (oldCursorStack.item.cost / 2)
		if (oldCursorStack.amount > amount) {
			campaign.cursorItemStack = ItemStack(
				oldCursorStack.item, oldCursorStack.amount - amount
			)
		} else campaign.cursorItemStack = null
		val oldShopStack = shopState.inventory[shopInventoryIndex]
		shopState.inventory[shopInventoryIndex] = ItemStack(
			oldCursorStack.item, (oldShopStack?.amount ?: 0) + amount
		)
	}

	override fun equals(other: Any?) = other is PendingSell && this.amount == other.amount &&
			this.shopInventoryIndex == other.shopInventoryIndex

	override fun hashCode() = amount - 17 * shopInventoryIndex
}
