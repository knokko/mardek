package mardek.game.action

import mardek.content.area.Direction
import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.actions.PendingBuyItem
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestShops {

	fun testGoznorItemsChapter1(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)
			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_shop_I" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(2, 3), Direction.Up,
			)
			state.campaign.state = areaState

			val defaultShopKeeper = areaState.area.objects.characters.find { it.directionalSprites?.name == "man1" }!!
			val mugbert = areaState.area.objects.characters.find { it.directionalSprites?.name == "mugbert_teen" }!!

			// Use the default shopkeeper in chapter 1
			// TODO CHAP2 Test that Mugbert becomes the shopkeeper
			assertNotNull(areaState.getCharacterState(defaultShopKeeper))
			assertNull(areaState.getCharacterState(mugbert))

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

			val nonTradeColors = arrayOf(
				Color(153, 153, 153), // Thrash can
				Color(204, 153, 0), // Gold icon
				Color(152, 152, 101), // The tunic of Mardek/Deugan
				Color(71, 117, 34), // The sprite of Deugan
			)
			val tradeColors = arrayOf(
				Color(238, 203, 127), // Item name color
				Color(42, 34, 22), // Amount background color
				Color(255, 203, 102), // Price text color
				Color(208, 193, 142), // Border color
			)
			val baseColors = arrayOf(
				Color(22, 13, 13), // Upper bar
				Color(131, 81, 38), // Upper bar text
				Color(238, 203, 127), // 'Shop' text
				Color(208, 193, 142), // Slot border color
				Color(128, 85, 38), // The weapon of Mardek
				Color(79, 50, 22), // The weapon of Deugan
			) + nonTradeColors
			val potionColor = arrayOf(Color(22, 50, 102))
			val greyedOutPotionColor = arrayOf(Color(84, 76, 89))
			val liquidSoundColor = arrayOf(Color(142, 112, 185))
			testRendering(
				state, 800, 600, "item-shop1",
				baseColors + potionColor + liquidSoundColor, greyedOutPotionColor,
			)

			val sounds = content.audio.fixedEffects.ui
			state.update(updateContext)
			assertSame(sounds.openMenu, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			val actions = (areaState.suspension as AreaSuspensionActions).actions
			assertEquals(123, state.campaign.gold) // Sanity check
			val interaction = actions.shopInteraction!!

			// Move mouse cursor to the Liquid Sound in the shop inventory, and initiate a pending buy action
			val rs = interaction.renderedShopInventory!!
			updateContext.input.postEvent(MouseMoveEvent(rs.startX + 5 * rs.slotSize, rs.startY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))

			// Initiate a pending buy action
			val liquidSound = content.items.items.find { it.displayName == "LiquidSound" }!!
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			assertEquals(PendingBuyItem(liquidSound), interaction.pendingTrade)
			assertSame(sounds.clickConfirm, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			testRendering(
				state, 800, 600, "item-shop2",
				liquidSoundColor + tradeColors, potionColor + nonTradeColors,
			)

			// Press the left arrow, which shouldn't have any effect
			assertEquals(0, interaction.inventory.descriptionIndex)
			assertEquals(1, interaction.pendingTrade!!.amount)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			assertEquals(0, interaction.inventory.descriptionIndex)
			assertEquals(1, interaction.pendingTrade!!.amount)

			// Press the right arrow 3 times, which should increase the pending amount to 4
			repeat(3) {
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
				state.update(updateContext)
				updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			}
			assertEquals(0, interaction.inventory.descriptionIndex)
			assertEquals(4, interaction.pendingTrade!!.amount)

			// Press the left arrow, which should decrease the pending amount to 3
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			assertEquals(3, interaction.pendingTrade!!.amount)

			// Press the down arrow, which should decrease the pending amount to 1
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			assertEquals(1, interaction.pendingTrade!!.amount)

			// Press the up arrow, which should increase the pending amount to the maximum
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			assertEquals(6, interaction.pendingTrade!!.amount)

			// Press E, which should buy the liquid sounds
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertSame(sounds.trade, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			assertEquals(3, state.campaign.gold)
			assertEquals(ItemStack(liquidSound, 6), state.campaign.cursorItemStack)

			// All shop items should be greyed out, since we no longer have the gold to buy any of them
			testRendering(
				state, 800, 600, "item-shop3",
				baseColors + liquidSoundColor + greyedOutPotionColor, potionColor,
			)

			// Try to put the liquid sounds in an accessory equipment slot, which should be forbidden
			val rc = interaction.renderedCharacterBars[0]
			updateContext.input.postEvent(MouseMoveEvent(
				rc.startX + 5 * rc.slotSpacing, rc.startY
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			assertSame(sounds.clickReject, updateContext.soundQueue.take())
			assertEquals(ItemStack(liquidSound, 6), state.campaign.cursorItemStack)

			// Put the liquid sounds in the inventory of Deugan
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			val ri = interaction.renderedCharacterInventory!!
			updateContext.input.postEvent(MouseMoveEvent(ri.startX, ri.startY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			assertSame(sounds.scroll2, updateContext.soundQueue.take())
			assertSame(sounds.clickCancel, updateContext.soundQueue.take())
			assertNull(state.campaign.cursorItemStack)

			val deuganInventory = state.campaign.characterStates[childDeugan]!!.inventory
			assertEquals(ItemStack(liquidSound, 6), deuganInventory[0])

			// Discard 1 of the 6 liquid sounds
			updateContext.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			val tr = interaction.thrashRegion!!
			updateContext.input.postEvent(MouseMoveEvent(tr.minX, tr.minY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.SplitClick))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			assertEquals(ItemStack(liquidSound, 5), deuganInventory[0])
			assertSame(sounds.clickConfirm, updateContext.soundQueue.take())
			assertSame(sounds.clickCancel, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// Take 3 of the 5 liquid sounds, and prepare to sell them
			updateContext.input.postEvent(MouseMoveEvent(ri.startX, ri.startY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.SplitClick))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.SplitClick))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.SplitClick))
			updateContext.input.postEvent(MouseMoveEvent(rs.startX, rs.startY + 5 * rs.slotSize))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.SplitClick))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			repeat(4) {
				assertSame(sounds.clickConfirm, updateContext.soundQueue.take())
			}

			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			assertSame(sounds.trade, updateContext.soundQueue.take())
			assertNull(state.campaign.cursorItemStack)
			assertEquals(33, state.campaign.gold)
			assertEquals(
				ItemStack(liquidSound, 3),
				state.campaign.shops.get(interaction.shop).inventory[30]
			)

			// The potion should be visible again
			testRendering(
				state, 800, 600, "item-shop4",
				baseColors + potionColor + liquidSoundColor, greyedOutPotionColor,
			)

			// Click on an empty slot, which shouldn't do much
			updateContext.input.postEvent(MouseMoveEvent(
				rs.startX + rs.slotSize, rs.startY + 5 * rs.slotSize
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			assertSame(sounds.clickReject, updateContext.soundQueue.take())
			assertEquals(33, state.campaign.gold)
			assertNull(state.campaign.cursorItemStack)

			// Buy 1 liquid sound back
			updateContext.input.postEvent(MouseMoveEvent(
				rs.startX, rs.startY + 5 * rs.slotSize
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			assertSame(sounds.clickConfirm, updateContext.soundQueue.take())
			assertSame(sounds.trade, updateContext.soundQueue.take())
			assertEquals(13, state.campaign.gold)
			assertEquals(ItemStack(liquidSound, 1), state.campaign.cursorItemStack)

			// Try to swap it with a potion, which is not possible
			updateContext.input.postEvent(MouseMoveEvent(rs.startX, rs.startY))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			assertSame(sounds.clickReject, updateContext.soundQueue.take())
			assertEquals(13, state.campaign.gold)
			assertEquals(ItemStack(liquidSound, 1), state.campaign.cursorItemStack)

			// If the player gets more gold, it should remain impossible
			state.campaign.gold = 500
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Click))
			assertSame(sounds.clickReject, updateContext.soundQueue.take())
			assertEquals(500, state.campaign.gold)
			assertEquals(ItemStack(liquidSound, 1), state.campaign.cursorItemStack)

			// One final check: we should be able to view the skills and attributes of items
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.update(updateContext)
			assertEquals(1, interaction.inventory.descriptionIndex)
			updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			state.update(updateContext)
			assertEquals(2, interaction.inventory.descriptionIndex)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))

			// Test that this doesn't crash
			for (characterState in state.campaign.characterStates.values) {
				characterState.currentLevel = 1
			}
			updateContext.saves.createSave(
				updateContext.content, state.campaign,
				state.campaignName, SaveFile.Type.Cheat,
			)

			updateContext.input.postEvent(pressKeyEvent(InputKey.Escape))
			state.update(updateContext)
			assertNull(areaState.suspension)
		}
	}
}
