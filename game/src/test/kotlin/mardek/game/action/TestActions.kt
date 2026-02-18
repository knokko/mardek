package mardek.game.action

import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetDefaultDialogueObject
import mardek.content.action.ActionToArea
import mardek.content.action.FixedActionNode
import mardek.content.area.Direction
import mardek.content.stats.StatusEffect
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.actions.AreaActionsState
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

object TestActions {

	val dialogueBoxColors = arrayOf(
		Color(88, 71, 46), // Q/L buttons
		Color(238, 203, 127), // display name color
		Color(208, 193, 142), // line color
	)

	val eButtonColors = arrayOf(
		Color(145, 137, 112), // E button
	)

	fun testSaveCrystalCancel(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			val areaState = AreaState(
				dragonLairEntry, state.campaign.story,
				AreaPosition(5, 3),
			)
			state.campaign.state = areaState

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)

			// Walk left towards the save crystal
			context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))

			repeat(200) {
				state.update(context)
			}

			context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			assertEquals(AreaPosition(4, 3), areaState.getPlayerPosition(0))
			assertEquals(Direction.Left, areaState.getPlayerDirection(0))

			val dragonLairColors = arrayOf(
				Color(13, 0, 22), // background color
				Color(77, 69, 95), // brick color
				Color(96, 199, 242), // crystal ring color
				Color(186, 255, 255), // crystal outline color
			)

			val choiceColors = arrayOf(
				Color(155, 192, 238),
				Color(69, 69, 69),
			)

			testRendering(
				state, 1000, 800, "crystal-dialogue0",
				dragonLairColors, dialogueBoxColors + choiceColors
			)

			assertNull(areaState.suspension)
			assertNull(context.soundQueue.take())

			// Give mardek some 'damage', which should get cured by the save crystal
			val mardekState = state.campaign.characterStates[heroMardek]!!
			mardekState.currentHealth = 5
			mardekState.currentMana = 1
			mardekState.activeStatusEffects.add(content.stats.statusEffects.find { it.flashName == "PSN" }!!)

			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(context)
			}
			sleep(1000) // Wait until the annoying blue flash is gone to prevent testRendering from getting flaky

			val actions = (areaState.suspension as AreaSuspensionActions).actions
			val talkNode1 = (actions.node as FixedActionNode).action as ActionTalk
			assertTrue(
				actions.shownDialogueCharacters < talkNode1.text.length * 0.4f,
				"Expected ${actions.shownDialogueCharacters} to be small",
			)

			// Check that the save crystal healed mardek
			val maxHealth = mardekState.determineMaxHealth(heroMardek.baseStats, emptySet())
			val maxMana = mardekState.determineMaxMana(heroMardek.baseStats, emptySet())
			assertEquals(maxHealth, mardekState.currentHealth)
			assertEquals(maxMana, mardekState.currentMana)
			assertEquals(emptySet<StatusEffect>(), mardekState.activeStatusEffects)

			assertSame(content.audio.fixedEffects.saveCrystal, context.soundQueue.take())
			assertNull(context.soundQueue.take())

			testRendering(
				state, 1000, 800, "crystal-dialogue1",
				dragonLairColors + dialogueBoxColors, choiceColors
			)

			// Test that pressing E (interact) again speeds the dialogue up
			context.input.postEvent(repeatKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(context)
			}
			assertEquals(talkNode1.text.length.toFloat(), actions.shownDialogueCharacters)

			// Pressing E once more should cause a transition to the choice node
			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(100) {
				state.update(context)
			}
			assertEquals(0f, actions.shownDialogueCharacters)

			assertEquals(2, actions.choiceOptions.size) // Only "Save" and "Exit" at this point
			assertEquals(0, actions.selectedChoice)

			context.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(100) {
				state.update(context)
				assertEquals(1, actions.selectedChoice)
			}

			testRendering(
				state, 1000, 800, "crystal-dialogue2",
				dragonLairColors + dialogueBoxColors + choiceColors, emptyArray()
			)

			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(context)
			assertFalse(areaState.suspension is AreaSuspensionActions)
			assertNull(context.soundQueue.take())
		}
	}

	fun testAreaToArea(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			assertEquals(Direction.Up, (state.campaign.state as AreaState).getPlayerDirection(0))

			val toHeroesDen = ActionToArea("heroes_den", 5, 6, Direction.Left)
			toHeroesDen.resolve(content.areas.areas)

			(state.campaign.state as AreaState).suspension = AreaSuspensionActions(AreaActionsState(
				node = FixedActionNode(id = UUID.randomUUID(), action = toHeroesDen, next = null),
				defaultDialogueObject = null,
			))

			val context = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 10.milliseconds
			)
			state.update(context)

			// Wait until the fade is over
			repeat(60) {
				state.update(context)
			}

			assertSame(
				content.areas.areas.find { it.properties.rawName == "heroes_den" }!!,
				(state.campaign.state as AreaState).area,
			)

			for (index in 0 until 4) {
				assertEquals(
					AreaPosition(5, 6),
					(state.campaign.state as AreaState).getPlayerPosition(index),
				)
				assertEquals(
					Direction.Left,
					(state.campaign.state as AreaState).getPlayerDirection(index),
				)
			}
			assertNull((state.campaign.state as AreaState).suspension)
		}
	}

	fun testGlobalActionsToArea(instance: TestingInstance) {
		instance.apply {
			val toHeroesDen = ActionToArea("heroes_den", 5, 6, Direction.Left)
			toHeroesDen.resolve(content.areas.areas)

			val state = InGameState(simpleCampaignState(), "")
			state.campaign.state = CampaignActionsState(FixedActionNode(
				id = UUID.randomUUID(), action = toHeroesDen, next = null
			))

			state.update(GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 10.milliseconds
			))

			assertSame(
				content.areas.areas.find { it.properties.rawName == "heroes_den" }!!,
				(state.campaign.state as AreaState).area,
			)

			for (index in 0 until 4) {
				assertEquals(
					AreaPosition(5, 6),
					(state.campaign.state as AreaState).getPlayerPosition(index),
				)
				assertEquals(
					Direction.Left,
					(state.campaign.state as AreaState).getPlayerDirection(index),
				)
			}
			assertNull((state.campaign.state as AreaState).suspension)
		}
	}

	fun testHeroesHouseWritings(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			state.campaign.state = AreaState(
				area = content.areas.areas.find { it.properties.rawName == "heroes_house" }!!,
				story = state.campaign.story,
				initialPlayerPosition = AreaPosition(3, 1),
				initialPlayerDirection = Direction.Up,
			)

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			state.update(context)
			assertNull((state.campaign.state as AreaState).suspension)

			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(context)
			val suspension = (state.campaign.state as AreaState).suspension as AreaSuspensionActions
			val node = suspension.actions.node as FixedActionNode
			val action = node.action as ActionTalk
			assertEquals("", action.expression)
			assertEquals(ActionTargetDefaultDialogueObject(), action.speaker)
			assertEquals("Advenshers", action.speaker.getDisplayName(
				suspension.actions.defaultDialogueObject, state.campaign.party
			))
			assertTrue(action.text.startsWith("It's a scrapbook containing Mardek and Deugan's"))

			// Test that this doesn't crash
			for (characterState in state.campaign.characterStates.values) {
				characterState.currentLevel = 1
			}
			context.saves.createSave(
				context.content, state.campaign,
				state.campaignName, SaveFile.Type.Cheat,
			)
		}
	}
}
