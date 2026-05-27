package mardek.game.ui

import mardek.content.inventory.ItemStack
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.menu.inventory.InventoryTab
import mardek.state.ingame.menu.MapTab
import mardek.state.ingame.menu.PartyTab
import mardek.state.ingame.menu.SkillsTab
import mardek.state.saves.SavesFolderManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestInGameMenu {

	fun testOpeningAndScrolling(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()

			val state = InGameState(campaign, "test")
			val input = InputManager()
			val stateManager = GameStateManager(input, state, SavesFolderManager())
			val soundQueue = SoundQueue()
			val context = GameStateUpdateContext(content, input, soundQueue, 10.milliseconds)
			val sounds = content.audio.fixedEffects.ui

			val areaColors = arrayOf(
				Color(78, 68, 94), // Light floor color
			)
			val partyColors = arrayOf(
				Color(217, 214, 214), // Mardek armor
				Color(70, 117, 33), // Deugan robe
			)
			val partyTabColors = arrayOf(
				Color(238, 203, 127), // Area name color
				Color(132, 81, 38), // Party text color
				Color(236, 197, 157), // Skin color for portraits
				Color(71, 133, 22), // Health bar
				Color(59, 42, 28), // Mana bar background
			)
			val skillsTabColors = arrayOf(
				Color(255, 230, 145), // Powers icon
				Color(232, 192, 104), // Star icon
				Color(100, 249, 150), // Passive icon
				Color(255, 255, 255), // Smite Evil element icon
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
				stateManager, 800, 450, "in-game-menu-before-open",
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
				stateManager, 800, 450, "in-game-menu-party-tab",
				partyTabColors, areaColors
			)

			input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is SkillsTab)
			assertSame(sounds.scroll1, soundQueue.take())
			assertNull(soundQueue.take())

			testRendering(
				stateManager, 1200, 800, "in-game-menu-skills-tab",
				skillsTabColors + partyColors, areaColors
			)

			input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is InventoryTab)
			assertSame(sounds.scroll1, soundQueue.take())
			assertNull(soundQueue.take())

			val mardekState = campaign.characterStates[heroMardek]!!
			mardekState.inventory[5] = ItemStack(content.items.items.find { it.displayName == "Potion" }!!, 1)
			mardekState.activeStatusEffects.add(content.stats.statusEffects.find { it.flashName == "NUM" }!!)
			val deuganState = campaign.characterStates[heroDeugan]!!
			deuganState.equipment[heroDeugan.characterClass.equipmentSlots[3]] = content.items.items.find { it.displayName == "Hero's Coat" }!!
			deuganState.inventory[0] = ItemStack(content.items.items.find { it.displayName == "MugwortJuice" }!!, 1)

			val mugwortJuiceColor = arrayOf(Color(101, 141, 0))
			testRendering(
				stateManager, 1200, 675, "in-game-menu-inventory-tab",
				inventoryTabColors + partyColors, areaColors + mugwortJuiceColor
			)

			input.postEvent(repeatKeyEvent(InputKey.MoveDown))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is MapTab)
			assertSame(sounds.scroll1, soundQueue.take())
			assertNull(soundQueue.take())

			val mapColors = arrayOf(
				Color(131, 113, 80), // Walkable color
				Color(81, 54, 35), // Unwalkable color
			)
			testRendering(
				stateManager, 900, 450, "in-game-menu-map-tab",
				mapColors, areaColors + partyColors
			)

			input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			assertFalse(state.menu.shown)
			assertTrue(state.menu.currentTab is MapTab)
			assertNull(soundQueue.take())

			testRendering(
				stateManager, 800, 450, "in-game-menu-before-open",
				areaColors + partyColors, partyTabColors
			)

			input.postEvent(repeatKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			assertTrue(state.menu.shown)
			assertTrue(state.menu.currentTab is MapTab)
			assertSame(sounds.openMenu, soundQueue.take())
			assertNull(soundQueue.take())

			testRendering(
				stateManager, 900, 450, "in-game-menu-map-tab",
				mapColors, areaColors + partyColors
			)
		}
	}

	fun testPartyTabs(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			val sounds = content.audio.fixedEffects.ui

			val baseColors = arrayOf(
				Color(217, 214, 214), // Mardek armor
				Color(70, 117, 33), // Deugan robe
				Color(236, 197, 157), // Skin color for portraits
				Color(165, 205, 254), // Selection border color
				Color(238, 203, 127), // Text color
				Color(141, 103, 49), // Clock color
				Color(131, 81, 38), // "PARTY" color
			)

			val tabColors0 = arrayOf(
				Color(71, 133, 22), // Health bar
				Color(59, 42, 28), // Mana bar background
			)

			val tabColors1 = arrayOf(
				Color(131, 118, 85), // Sword icon color
				Color(152, 255, 0), // Green text color
				Color(95, 127, 141), // Spell cast background color
			)

			val tabColors2 = arrayOf(
				Color(79, 75, 90), // Positive resistance background color
				Color(110, 61, 38), // Negative resistance background color
				Color(0, 255, 0), // Earth element background
			)

			val tabColors3 = arrayOf(
				Color(110, 97, 81) // Greyed-out silence icon
			)

			val tabColors4 = arrayOf(
				Color(59, 42, 28), // Experience bar background
				Color(253, 221, 95), // Text color
			)

			val tabColors5 = arrayOf(
				Color(77, 113, 120) // Skills cast icon
			)

			context.input.postEvent(pressKeyEvent(InputKey.ToggleMenu))
			context.input.postEvent(releaseKeyEvent(InputKey.ToggleMenu))
			state.update(context)
			assertTrue(state.menu.shown)
			assertEquals(0, (state.menu.currentTab as PartyTab).currentTab)
			assertSame(sounds.openMenu, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			testRendering(
				state, 800, 500, "party-tab0",
				baseColors + tabColors0, emptyArray(),
			)

			context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(context)
			assertEquals(6, (state.menu.currentTab as PartyTab).currentTab)
			testRendering(
				state, 800, 500, "party-tab6",
				baseColors, emptyArray(),
			)
			assertSame(sounds.scroll2, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
			state.update(context)
			assertEquals(5, (state.menu.currentTab as PartyTab).currentTab)
			testRendering(
				state, 800, 500, "party-tab5",
				baseColors + tabColors5, emptyArray(),
			)
			assertSame(sounds.scroll2, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(repeatKeyEvent(InputKey.MoveLeft))
			state.update(context)
			assertEquals(4, (state.menu.currentTab as PartyTab).currentTab)
			testRendering(
				state, 800, 500, "party-tab4",
				baseColors + tabColors4, emptyArray(),
			)
			assertSame(sounds.scroll2, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(3) {
				context.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			}
			state.update(context)
			assertEquals(1, (state.menu.currentTab as PartyTab).currentTab)
			testRendering(
				state, 800, 500, "party-tab1",
				baseColors + tabColors1, emptyArray(),
			)
			repeat(4) {
				assertSame(sounds.scroll2, context.soundQueue.take())
			}
			assertNull(context.soundQueue.take())

			context.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			state.update(context)
			assertEquals(2, (state.menu.currentTab as PartyTab).currentTab)
			testRendering(
				state, 800, 500, "party-tab2",
				baseColors + tabColors2, emptyArray(),
			)
			assertSame(sounds.scroll2, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			context.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			state.update(context)
			assertEquals(3, (state.menu.currentTab as PartyTab).currentTab)
			testRendering(
				state, 800, 500, "party-tab3",
				baseColors + tabColors3, emptyArray(),
			)
			assertSame(sounds.scroll2, context.soundQueue.take())
			assertNull(context.soundQueue.take())
		}
	}

	fun testSkills(instance: TestingInstance) {
		instance.apply {
			val campaign = simpleCampaignState()
			val deugan = campaign.characterStates[heroDeugan]!!
			val snakeBite = content.skills.reactionSkills.find { it.name == "Snakebite" }!!
			val smiteEvil = heroDeugan.characterClass.skillClass.actions.find { it.name == "Smite Evil" }!!
			val increaseDamage = content.skills.reactionSkills.find { it.name == "DMG+50%" }!!
			deugan.skillMastery[snakeBite] = 10
			deugan.skillMastery[smiteEvil] = 10
			deugan.skillMastery[increaseDamage] = 10
			deugan.toggledSkills.add(snakeBite)

			val state = InGameState(campaign, "test")
			val input = InputManager()
			val stateManager = GameStateManager(input, state, SavesFolderManager())
			val soundQueue = SoundQueue()
			val context = GameStateUpdateContext(content, input, soundQueue, 10.milliseconds)
			val sounds = content.audio.fixedEffects.ui

			val baseColors = arrayOf(
				Color(217, 214, 214), // Mardek armor
				Color(70, 117, 33), // Deugan robe
				Color(165, 205, 254), // Selection border color
				Color(255, 230, 145), // Powers icon
				Color(232, 192, 104), // Star icon
				Color(208, 192, 142), // Melee reactions icon
				Color(99, 249, 249), // Magic reactions icon
				Color(100, 249, 150), // Passive icon
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
			assertSame(sounds.scroll1, soundQueue.take())
			assertSame(sounds.scroll1, soundQueue.take())
			assertNull(soundQueue.take())

			input.postEvent(pressKeyEvent(InputKey.Interact))
			input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(context)
			assertTrue(state.menu.currentTab.inside)
			assertSame(sounds.clickConfirm, soundQueue.take())
			assertNull(soundQueue.take())

			val actionColors = arrayOf(
				Color(219, 218, 177), // Slightly translucent air element icon
				Color(25, 219, 8), // Slightly translucent earth element icon
				Color(34, 247, 255), // Mana text color
				Color(229, 228, 136), // Mastery text border color
			)
			val disabledColors = arrayOf(
				Color(124, 60, 49), // Disabled mastery bar color
			)
			val reactionColors = arrayOf(
				Color(159, 39, 30), // Mastery bar color
				Color(26, 219, 87), // Toggled color
			)
			val rpColor = arrayOf(Color(15, 145, 32))

			testRendering(
				stateManager, 1600, 900, "skills-deugan-active",
				baseColors + actionColors + disabledColors, reactionColors + rpColor
			)

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
				stateManager, 1600, 900, "skills-deugan-reactions",
				baseColors + reactionColors + rpColor + disabledColors, actionColors
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
				stateManager, 1600, 900, "skills-deugan-passives",
				baseColors + rpColor, actionColors + reactionColors + disabledColors
			)

			input.postEvent(pressKeyEvent(InputKey.Cancel))
			state.update(context)
			assertTrue(state.menu.shown)
			assertFalse(state.menu.currentTab.inside)
			assertInstanceOf<SkillsTab>(state.menu.currentTab)

			input.postEvent(repeatKeyEvent(InputKey.Cancel))
			state.update(context)
			assertFalse(state.menu.shown)
		}
	}
}
