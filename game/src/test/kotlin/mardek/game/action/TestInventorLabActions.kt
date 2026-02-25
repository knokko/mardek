package mardek.game.action

import mardek.content.area.Direction
import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestInventorLabActions {

	fun testLeadPipeQuest(instance: TestingInstance) {
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
				content.areas.areas.find { it.properties.rawName == "gz_house02" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(3, 3), Direction.Up,
			)
			state.campaign.state = areaState

			val quest = content.story.quests.find { it.tabName == "LeadPipes" }!!
			assertFalse(state.campaign.story.getQuests(content.story).active.contains(quest))

			// First decline the quest
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			assertFalse(state.campaign.story.getQuests(content.story).active.contains(quest))

			// Talk again, but accept the quest this time
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			assertTrue(state.campaign.story.getQuests(content.story).active.contains(quest))
			assertFalse(state.campaign.story.getQuests(content.story).completed.contains(quest))

			// Put enough lead pipes in the *item storage*, which is irrelevant
			val leadPipe = content.items.items.find { it.displayName == "LeadPipe" }!!
			state.campaign.itemStorage.add(ItemStack(leadPipe, 10))

			// So if we talk to Mereador again, we should NOT be able to complete the quest
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			var actions = (areaState.suspension as AreaSuspensionActions).actions
			assertEquals(1, actions.choiceOptions.size)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			assertTrue(state.campaign.story.getQuests(content.story).active.contains(quest))
			assertFalse(state.campaign.story.getQuests(content.story).completed.contains(quest))

			// Try again, but this time with enough lead pipes in the main inventory
			val mardekState = state.campaign.characterStates[childMardek]!!
			mardekState.inventory[3] = ItemStack(leadPipe, 1)
			mardekState.inventory[5] = ItemStack(leadPipe, 2)

			val deuganState = state.campaign.characterStates[childDeugan]!!
			deuganState.inventory[7] = ItemStack(leadPipe, 1)
			deuganState.inventory[12] = ItemStack(leadPipe, 3)

			val cogNecklace = content.items.items.find { it.displayName == "Cog Necklace" }!!
			assertEquals(7, state.campaign.expressionContext().countItemInInventory(leadPipe))
			assertEquals(0, state.campaign.expressionContext().countItemInInventory(cogNecklace))

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			actions = (areaState.suspension as AreaSuspensionActions).actions

			// Choose to give Mereador the lead pipes, and finish the quest.
			// Slow the dialogue down, and check that the item notifications are renderer properly.
			assertEquals(2, actions.choiceOptions.size)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

			state.update(updateContext)
			repeat(150) {
				state.update(updateContext)
			}

			val baseColors = arrayOf(
				Color(235, 217, 168), // Books/sheets
				Color(106, 88, 56), // Floor
				Color(255, 232, 124), // Mereador area sprite
				Color(238, 203, 127), // Speaker text color
			)
			val mereadorPortraitColors = arrayOf(
				Color(131, 81, 37),
				Color(102, 51, 51),
			)
			val leadPipeColors = arrayOf(
				Color(94, 126, 4),
				Color(133, 110, 97),
			)
			val necklaceColors = arrayOf(
				Color(173, 132, 4),
				Color(85, 42, 4),
			)
			testRendering(
				state, 600, 400, "lead-pipe-quest0",
				baseColors + mereadorPortraitColors, leadPipeColors + necklaceColors,
			)

			// Go to the dialogue entry that should show the 'lost lead pipes' notification
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(5) {
				state.update(updateContext)
			}
			testRendering(
				state, 600, 400, "lead-pipe-quest1",
				baseColors + mereadorPortraitColors + leadPipeColors, necklaceColors,
			)

			// Go to the next dialogue entry, where nothing interesting happens
			repeat(150) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(150) {
				state.update(updateContext)
			}

			// Go to the dialogue entry where the cog necklace is shown
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(5) {
				state.update(updateContext)
			}
			testRendering(
				state, 600, 400, "lead-pipe-quest2",
				baseColors + necklaceColors, leadPipeColors,
			)

			// Test that it fades after some time
			repeat(150) {
				state.update(updateContext)
			}
			testRendering(
				state, 600, 400, "lead-pipe-quest3",
				baseColors, necklaceColors + leadPipeColors,
			)

			// Skip the rest of the dialogue
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(50) {
				state.update(updateContext)
			}

			assertNull(areaState.suspension)
			assertFalse(state.campaign.story.getQuests(content.story).active.contains(quest))
			assertTrue(state.campaign.story.getQuests(content.story).completed.contains(quest))
			assertEquals(2, state.campaign.expressionContext().countItemInInventory(leadPipe))
			assertEquals(1, state.campaign.expressionContext().countItemInInventory(cogNecklace))
			assertEquals(0, mardekState.countItemOccurrences(leadPipe))
			assertEquals(1, mardekState.countItemOccurrences(cogNecklace))
			assertEquals(2, deuganState.countItemOccurrences(leadPipe))
			assertEquals(0, deuganState.countItemOccurrences(cogNecklace))

			// Talk to Mereador again, who shouldn't have anything interesting left to say
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertInstanceOf<AreaSuspensionActions>(areaState.suspension)
			repeat(50) {
				state.update(updateContext)
			}
			assertNull(areaState.suspension)
			assertFalse(state.campaign.story.getQuests(content.story).active.contains(quest))
			assertTrue(state.campaign.story.getQuests(content.story).completed.contains(quest))
			assertEquals(2, state.campaign.expressionContext().countItemInInventory(leadPipe))
			assertEquals(1, state.campaign.expressionContext().countItemInInventory(cogNecklace))
		}
	}
}
