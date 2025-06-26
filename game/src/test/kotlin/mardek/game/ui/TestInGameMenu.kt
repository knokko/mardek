package mardek.game.ui

import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.renderer.SharedResources
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.menu.InventoryTab
import mardek.state.ingame.menu.MapTab
import mardek.state.ingame.menu.PartyTab
import mardek.state.ingame.menu.SkillsTab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

object TestInGameMenu {

	fun testOpeningAndScrolling(instance: TestingInstance) {
		instance.apply {
			val getResources = CompletableFuture<SharedResources>()
			getResources.complete(SharedResources(getBoiler, 1, skipWindow = true))

			val campaign = simpleCampaignState()

			val state = InGameState(campaign)
			val input = InputManager()
			val soundQueue = SoundQueue()
			val context = GameStateUpdateContext(content, input, soundQueue, 10.milliseconds)
			val sounds = content.audio.fixedEffects.ui

			val areaColors = arrayOf(
				Color(77, 66, 93), // Light floor color
				Color(59, 53, 66), // Dark floor color
			)
			val partyColors = arrayOf(
				Color(217, 214, 214), // Mardek armor
				Color(69, 117, 28), // Deugan robe
			)
			val partyTabColors = arrayOf(
				Color(238, 203, 127), // Area name color
				Color(22, 13, 13), // Dark bar color
				Color(132, 81, 38), // Party text color
			)
			val skillsTabColors = arrayOf(
				Color(255, 230, 145), // Powers icon
				Color(232, 192, 104), // Star icon
				Color(102, 239, 142), // Passive icon
				Color(255, 255, 255), // Smite Evil element icon
				Color(253, 95, 95), // Mastery text icon
				Color(34, 247, 255), // Mana text icon
				Color(165, 205, 254), // Party highlight color
			)
			val inventoryTabColors = arrayOf(
				Color(179, 162, 117), // Inventory grid color
				Color(255, 203, 101), // Balmung handle color
				Color(0, 66, 13), // Hero's Coat color
				Color(223, 223, 223), // M. Blade color
				Color(59, 42, 28), // Empty mana bar color
				Color(0, 141, 222), // Potion color
				Color(153, 0, 0), // Numbness color
			)
			testRendering(
				getResources, state, 800, 450, "in-game-menu-before-open",
				areaColors + partyColors, partyTabColors
			)

			state.update(context)
			assertFalse(state.menu.shown)
			assertNull(soundQueue.take())

			input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is PartyTab)
			assertSame(sounds.openMenu, soundQueue.take())
			assertNull(soundQueue.take())

			testRendering(
				getResources, state, 800, 450, "in-game-menu-party-tab",
				partyTabColors, partyColors + areaColors
			)

			input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is SkillsTab)
			assertSame(sounds.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			testRendering(
				getResources, state, 800, 450, "in-game-menu-skills-tab",
				skillsTabColors + partyColors, areaColors
			)

			input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is InventoryTab)
			assertSame(sounds.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.inventory[5] = ItemStack(content.items.items.find { it.flashName == "Potion" }!!, 1)
			mardekState.activeStatusEffects.add(content.stats.statusEffects.find { it.flashName == "NUM" }!!)
			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.equipment[3] = content.items.items.find { it.flashName == "Hero's Coat" }!!
			deuganState.inventory[0] = ItemStack(content.items.items.find { it.flashName == "MugwortJuice" }!!, 1)

			val mugwortJuiceColor = arrayOf(Color(101, 141, 0))
			testRendering(
				getResources, state, 1200, 675, "in-game-menu-inventory-tab",
				inventoryTabColors + partyColors, areaColors + mugwortJuiceColor
			)

			input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is MapTab)
			assertSame(sounds.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			val mapColors = arrayOf(
				Color(131, 113, 80), // Walkable color
				Color(81, 54, 35), // Unwalkable color
			)
			testRendering(
				getResources, state, 900, 450, "in-game-menu-map-tab",
				mapColors, areaColors + partyColors
			)

			input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			assertFalse(state.menu.shown)
			assertTrue(state.menu.currentTab is MapTab)
			assertNull(soundQueue.take())

			testRendering(
				getResources, state, 800, 450, "in-game-menu-before-open",
				areaColors + partyColors, partyTabColors
			)

			input.postEvent(repeatKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is MapTab)
			assertSame(sounds.openMenu, soundQueue.take())
			assertNull(soundQueue.take())

			testRendering(
				getResources, state, 900, 450, "in-game-menu-map-tab",
				mapColors, areaColors + partyColors
			)

			getResources.get().destroy()
		}
	}

	fun testSkills(instance: TestingInstance) {
		instance.apply {
			val getResources = CompletableFuture<SharedResources>()
			getResources.complete(SharedResources(getBoiler, 1, skipWindow = true))

			val campaign = simpleCampaignState()
			val deugan = campaign.characterStates[heroDeugan]!!
			val snakeBite = content.skills.reactionSkills.find { it.name == "Snakebite" }!!
			deugan.skillMastery[snakeBite] = 10
			deugan.toggledSkills.add(snakeBite)

			val state = InGameState(campaign)
			val input = InputManager()
			val soundQueue = SoundQueue()
			val context = GameStateUpdateContext(content, input, soundQueue, 10.milliseconds)
			val sounds = content.audio.fixedEffects.ui

			val baseColors = arrayOf(
				Color(217, 214, 214), // Mardek armor
				Color(69, 117, 28), // Deugan robe
				Color(165, 205, 254), // Selection border color
				Color(255, 230, 145), // Powers icon
				Color(232, 192, 104), // Star icon
				Color(208, 192, 142), // Melee reactions icon
				Color(99, 249, 249), // Magic reactions icon
				Color(102, 239, 142), // Passive icon
				Color(50, 38, 28), // Ability bar icon,
				Color(238, 203, 127), // Text color
			)

			input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			assertTrue(state.menu.shown)
			input.postEvent(pressKeyEvent(InputKey.MoveDown))
			input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			state.update(context)
			assertTrue(state.menu.shown)
			assertEquals(1, (state.menu.currentTab as SkillsTab).partyIndex)
			assertFalse(state.menu.currentTab.inside)
			assertSame(sounds.openMenu, soundQueue.take())
			assertSame(sounds.scroll, soundQueue.take())
			assertSame(sounds.scroll, soundQueue.take())
			assertNull(soundQueue.take())

			val actionColors = arrayOf(
				Color(254, 254, 194), // Air element icon
				Color(14, 243, 8), // Earth element icon
				Color(11, 195, 243), // Water element icon
				Color(34, 247, 255), // Mana text color
				Color(255, 147, 22), // Mastery text icon
			)
			val reactionColors = arrayOf(
				Color(159, 28, 22), // Mastery bar color
				Color(22, 13, 0), // Not toggled color
				Color(0, 255, 101), // Toggled color
			)
			val rpColor = arrayOf(Color(22, 241, 71))

			testRendering(
				getResources, state, 1600, 900, "skills-deugan-active",
				baseColors + actionColors, reactionColors + rpColor
			)

			input.postEvent(pressKeyEvent(InputKey.Interact))
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			input.postEvent(pressKeyEvent(InputKey.MoveDown))
			input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			state.update(context)
			assertTrue(state.menu.currentTab.inside)
			assertEquals(1, (state.menu.currentTab as SkillsTab).skillIndex)

			input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.update(context)

			assertTrue(state.menu.currentTab.inside)
			assertEquals(1, (state.menu.currentTab as SkillsTab).skillTypeIndex)
			assertEquals(1, (state.menu.currentTab as SkillsTab).skillIndex)
			testRendering(
				getResources, state, 1600, 900, "skills-deugan-reactions",
				baseColors + reactionColors + rpColor, actionColors
			)

			input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			state.update(context)
			assertEquals(5, (state.menu.currentTab as SkillsTab).skillTypeIndex)
			assertEquals(0, (state.menu.currentTab as SkillsTab).skillIndex)
			testRendering(
				getResources, state, 1600, 900, "skills-deugan-passives",
				baseColors + rpColor, actionColors + reactionColors
			)

			input.postEvent(pressKeyEvent(InputKey.Cancel))
			state.update(context)
			assertTrue(state.menu.shown)
			assertFalse(state.menu.currentTab.inside)
			assertInstanceOf<SkillsTab>(state.menu.currentTab)

			input.postEvent(repeatKeyEvent(InputKey.Cancel))
			state.update(context)
			assertFalse(state.menu.shown)

			getResources.join().destroy()
		}
	}
}
