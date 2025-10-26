package mardek.game.action

import mardek.content.action.ActionTalk
import mardek.content.action.ActionTargetPartyMember
import mardek.content.action.ActionWalk
import mardek.content.action.ChoiceActionNode
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
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds

object TestActions {

	private val dialogueBoxColors = arrayOf(
		Color(88, 71, 46), // Q/L buttons
		Color(238, 203, 127), // display name color
		Color(208, 193, 142), // line color
		Color(145, 137, 112), // E button
	)

	fun testIntroDialogue(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(CampaignState(
				currentArea = AreaState(dragonLairEntry, AreaPosition(5, 10)),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			), "test")

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			state.update(context)

			val dragonLairColors = arrayOf(
				Color(13, 0, 22), // background color
				Color(77, 69, 95), // brick color
			)

			testRendering(
				state, 1000, 800, "intro-dialogue0",
				dragonLairColors, dialogueBoxColors
			)

			// This should have no effect since the player is forced to walk
			context.input.postEvent(pressKeyEvent(InputKey.MoveLeft))

			val areaState = state.campaign.currentArea!!
			val actions = areaState.actions!!
			assertTrue(actions.node is FixedActionNode)
			assertTrue((actions.node as FixedActionNode).action is ActionWalk)

			repeat(500) {
				state.update(context)
			}

			val mardekTalk1 = (actions.node as FixedActionNode).action as ActionTalk
			assertEquals(ActionTargetPartyMember(0), mardekTalk1.speaker)
			assertEquals("norm", mardekTalk1.expression)
			assertEquals("Well Deugan, this is The Dragon's Lair.", mardekTalk1.text)
			assertEquals(actions.shownDialogueCharacters, mardekTalk1.text.length.toFloat())

			assertEquals(AreaPosition(5, 5), areaState.getPlayerPosition(0))
			assertEquals(AreaPosition(5, 5), actions.partyPositions[0])
			assertEquals(Direction.Down, areaState.getPlayerDirection(0))
			assertEquals(Direction.Down, actions.partyDirections[0])
			assertEquals(AreaPosition(5, 6), actions.partyPositions[1])
			assertEquals(Direction.Up, actions.partyDirections[1])

			testRendering(
				state, 1000, 800, "intro-dialogue1",
				dragonLairColors + dialogueBoxColors, emptyArray()
			)

			// Go to next dialogue node
			context.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			context.input.postEvent(releaseKeyEvent(InputKey.Interact))
			state.update(context)

			val deuganTalk1 = (actions.node as FixedActionNode).action as ActionTalk
			assertEquals(ActionTargetPartyMember(1), deuganTalk1.speaker)
			assertEquals("grin", deuganTalk1.expression)
			assertEquals("Well Deugan, this is The Dragon's Lair.", mardekTalk1.text)
			assertTrue(
				actions.shownDialogueCharacters < 5f,
				"Expected ${actions.shownDialogueCharacters} to be small",
			)

			// Ideally, I would test that we render a different portrait, but I can't find reliable colors to test...
			testRendering(
				state, 1000, 800, "intro-dialogue2",
				dragonLairColors + dialogueBoxColors, emptyArray()
			)

			// Skip the rest of the dialogue
			context.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(500) {
				state.update(context)
			}

			assertNull(areaState.actions)
			assertEquals(AreaPosition(5, 5), areaState.getPlayerPosition(0))
			assertEquals(Direction.Down, areaState.getPlayerDirection(0))
			assertEquals(AreaPosition(5, 6), areaState.getPlayerPosition(1))
			assertEquals(Direction.Up, areaState.getPlayerDirection(1))
			testRendering(
				state, 1000, 800, "intro-dialogue3",
				dragonLairColors, dialogueBoxColors
			)

			// Test that we can walk away now that the dialogue is over
			context.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(200) {
				state.update(context)
			}

			assertEquals(AreaPosition(10, 5), areaState.getPlayerPosition(0))
		}
	}

	fun testSaveCrystalCancel(instance: TestingInstance) {
		instance.apply {
			val areaState = AreaState(dragonLairEntry, AreaPosition(5, 3))
			val state = InGameState(CampaignState(
				currentArea = areaState,
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			), "test")

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

			assertNull(areaState.actions)
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

			val actions = areaState.actions!!
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

			val choiceNode = actions.node as ChoiceActionNode
			assertEquals(2, choiceNode.options.size) // Only "Save" and "Exit" at this point
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
			assertNull(areaState.actions)
			assertNull(context.soundQueue.take())
		}
	}
}
