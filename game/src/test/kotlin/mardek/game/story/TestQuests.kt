package mardek.game.story

import mardek.content.story.Quest
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.menu.QuestsTab
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestQuests {

	fun testHeroQuest(instance: TestingInstance) {
		instance.apply {
			val state = CampaignState.loadChapter(content, 1)
			val quests = state.story.getQuests(content.story)
			assertArrayEquals(emptyArray<Quest>(), quests.completed)
			assertArrayEquals(
				arrayOf(content.story.quests.find { it.tabName == "Hero Quest!" }!!),
				quests.active
			)
		}
	}

	fun testChildhoodQuests(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"LeadPipeQuestTimeline", "Accepted Pipe Quest"
			)
			val sounds = content.audio.fixedEffects.ui

			// Open in-game menu
			updateContext.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			assertFalse(state.menu.shown)
			state.update(updateContext)
			assertTrue(state.menu.shown)
			assertSame(sounds.openMenu, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// Scroll down to quests tab
			repeat(4) {
				updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
				state.update(updateContext)
				assertSame(sounds.scroll1, updateContext.soundQueue.take())
				updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
				state.update(updateContext)
				assertNull(updateContext.soundQueue.take())
			}

			// Check that the quests are correct
			assertTrue(state.menu.shown)
			val tab = state.menu.currentTab as QuestsTab

			val heroQuest = content.story.quests.find { it.tabName == "Hero Quest!" }!!
			val starQuest = content.story.quests.find { it.tabName == "The Fallen Star" }!!
			val pipesQuest = content.story.quests.find { it.tabName == "LeadPipes" }!!
			assertArrayEquals(arrayOf(heroQuest), tab.quests.completed)
			assertEquals(setOf(starQuest, pipesQuest), tab.quests.active.toSet())

			val baseColors = arrayOf(
				Color(22, 13, 13), // Upper bar color
				Color(131, 81, 38), // "Quests" title
				Color(51, 153, 204), // Crystal pointer
				Color(165, 205, 254), // The currently-selected ACTIVE vs COMPLETED text/bar color
				Color(208, 193, 142), // The unselected text/bar color
				Color(141, 103, 49), // Clock color
				Color(138, 123, 87), // Parchment color
			)
			val diveOnlyColor = arrayOf(
				Color(255, 255, 152),
			)
			testRendering(
				state, 1000, 700, "quests1",
				baseColors, diveOnlyColor,
			)

			// Dive into the active quests
			assertFalse(tab.inside)
			assertFalse(tab.showCompleted)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertTrue(tab.inside)
			assertFalse(tab.showCompleted)
			assertEquals(0, tab.questIndex)
			assertSame(sounds.clickConfirm, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			testRendering(
				state, 1000, 700, "quests2",
				baseColors + diveOnlyColor, arrayOf(),
			)

			// Scroll to the next quest
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			assertTrue(tab.inside)
			assertFalse(tab.showCompleted)
			assertEquals(1, tab.questIndex)
			assertSame(sounds.scroll1, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// Try to scroll further down, which is not possible
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			assertNull(updateContext.soundQueue.take())
			assertTrue(tab.inside)
			assertFalse(tab.showCompleted)
			assertEquals(1, tab.questIndex)

			// Press cancel, and scroll right, to the completed quests
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.update(updateContext)
			assertFalse(tab.inside)
			assertTrue(tab.showCompleted)
			assertSame(sounds.clickCancel, updateContext.soundQueue.take())
			assertSame(sounds.scroll2, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// Dive into the completed quests
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertTrue(tab.inside)
			assertTrue(tab.showCompleted)
			assertEquals(0, tab.questIndex)
			assertSame(sounds.clickConfirm, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())

			// Try to scroll up, which is not possible
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			state.update(updateContext)
			assertTrue(tab.inside)
			assertTrue(tab.showCompleted)
			assertEquals(0, tab.questIndex)
			assertNull(updateContext.soundQueue.take())

			// Final sanity check
			assertSame(tab, state.menu.currentTab)
		}
	}
}
