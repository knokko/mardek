package mardek.game.action

import mardek.content.action.ActionTalk
import mardek.content.action.ChoiceActionNode
import mardek.content.action.FixedActionNode
import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertInstanceOf
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestMardekHouseActions {

	fun testMotherDialogueAfterDragonLair(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Dropped Deugan home before the falling 'star'"
			)
			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				state.campaign.story, AreaPosition(5, 21),
				Direction.Up,
			)

			// Enter the door
			assertEquals("crickets", state.campaign.determineMusicTrack(content))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

			// Which should automatically enter the action sequence where Mardek talks to his mum
			// Pressing the right arrow key does *not* prevent this
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(50) {
				state.update(updateContext)
			}
			assertNull(state.campaign.determineMusicTrack(content))
			val areaState = state.campaign.state as AreaState

			assertEquals(AreaPosition(3, 3), areaState.getPlayerPosition(0))
			assertEquals(Direction.Up, areaState.getPlayerDirection(0))

			val mother = areaState.area.objects.characters.find { it.name == "Lilanea" }!!
			assertEquals(Direction.Down, areaState.getCharacterState(mother)!!.direction)

			// Skip the dialogue until the first choice node
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(50) {
				state.update(updateContext)
			}

			val actions = (areaState.suspension as AreaSuspensionActions).actions
			assertInstanceOf<ChoiceActionNode>(actions.node)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

			// Skip until the second/last choice node
			repeat(50) {
				state.update(updateContext)
			}
			assertInstanceOf<ChoiceActionNode>(actions.node)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

			// Skip until the Enki music starts playing
			@Suppress("unused")
			for (counter in 0 until 50) {
				state.update(updateContext)
				if (actions.overrideMusic != null) break
			}

			assertEquals("Enki", state.campaign.determineMusicTrack(content))
			assertSame(content.actions.backgroundImages.find { it.name == "Enki Art" }!!, actions.backgroundImage)

			val dialogueColors = arrayOf(
				Color(236, 197, 157), // Portrait skin color
				Color(238, 203, 127), // Title color
			)
			val houseColors = arrayOf(
				Color(185, 168, 130), // Wall
				Color(59, 104, 22), // Mother dress
			)
			testRendering( // The house should be hidden behind the Enki Art
				state, 600, 400, "mum-dialogue1",
				dialogueColors, houseColors,
			)

			// Test that saving now doesn't cause a crash
			state.campaign.characterStates[heroMardek]!!.currentLevel = 1
			state.campaign.characterStates[heroDeugan]!!.currentLevel = 1
			dummySaveManager().createSave(
				content, state.campaign,
				"don't crash", SaveFile.Type.Cheat,
			)

			// Skip until the Enki music stops
			@Suppress("unused")
			for (counter in 0 until 50) {
				state.update(updateContext)
				if (actions.backgroundImage == null) break
			}

			assertSame(actions, (areaState.suspension as AreaSuspensionActions).actions)
			assertNull(state.campaign.determineMusicTrack(content))

			testRendering(
				state, 600, 400, "mum-dialogue2",
				dialogueColors + houseColors, arrayOf(),
			)

			// Skip until the dialogue is finished
			repeat(50) {
				state.update(updateContext)
			}

			assertNull(areaState.suspension)
			assertEquals(Direction.Left, areaState.getCharacterState(mother)!!.direction)
			assertEquals(AreaPosition(3, 3), areaState.getPlayerPosition(0))
			assertEquals(Direction.Up, areaState.getPlayerDirection(0))

			// Move back to the door, and check that the cutscene does NOT trigger again
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(20) {
				state.update(updateContext)
			}
			assertNull(areaState.suspension)
			assertEquals(AreaPosition(6, 5), areaState.getPlayerPosition(0))
		}
	}

	fun testFallingStarCutsceneAndDialogue(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Dropped Deugan home before the falling 'star'"
			)
			var areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_Mhouse2" }!!,
				state.campaign.story, AreaPosition(1, 1),
				Direction.Up,
			)
			state.campaign.state = areaState

			// Nothing should happen until Mardek steps in this bed
			repeat(5) {
				state.update(updateContext)
			}
			assertNull(areaState.suspension)

			// Walk into his bed
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			repeat(3) {
				state.update(updateContext)
			}

			// Mardek should close his eyes, and the area should start to fade
			assertEquals(Direction.Sleep, areaState.getPlayerDirection(0))
			assertEquals(AreaPosition(0, 1), areaState.getPlayerPosition(0))
			assertInstanceOf<AreaSuspensionActions>(areaState.suspension)

			// Wait until the cutscene starts
			repeat(25) {
				state.update(updateContext)
			}
			val campaignActions = state.campaign.state as CampaignActionsState

			val roomColors = arrayOf(
				Color(185, 168, 130), // Wall
				Color(195, 157, 79), // Deugan hair
				Color(90, 73, 42), // Floor
			)
			val cutsceneColors = arrayOf(
				Color(93, 171, 187), // Belfan sea color
				Color(105, 163, 124), // Belfan land color
			)
			testRendering(
				state, 1000, 800, "falling-star-1",
				cutsceneColors, roomColors,
			)

			// Force the cutscene to end
			campaignActions.finishedAnimationNode = true
			assertEquals(1, state.campaign.usedPartyMembers().size)
			repeat(3) {
				state.update(updateContext)
			}

			areaState = state.campaign.state as AreaState
			assertEquals(Direction.Sleep, areaState.getPlayerDirection(0))
			assertEquals(AreaPosition(3, 5), areaState.getPlayerPosition(1))
			assertEquals(2, state.campaign.usedPartyMembers().size)

			// Wait until Deugan stands next to the bed (the player should NOT be able to move in the meantime)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(50) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			assertEquals(AreaPosition(1, 1), areaState.getPlayerPosition(1))
			assertEquals(Direction.Left, areaState.getPlayerDirection(1))
			assertEquals(AreaPosition(0, 1), areaState.getPlayerPosition(0))
			assertEquals(Direction.Down, areaState.getPlayerDirection(0))

			val actions = (areaState.suspension as AreaSuspensionActions).actions
			assertInstanceOf<ActionTalk>((actions.node as FixedActionNode).action)

			// Skip the dialogue
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(50) {
				state.update(updateContext)
			}
			assertNull(areaState.suspension)

			// Move out of bed and go back in: check that the cutscene does NOT replay
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(5) {
				state.update(updateContext)
			}
			assertNotEquals(AreaPosition(0, 1), areaState.getPlayerPosition(0))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			repeat(10) {
				state.update(updateContext)
			}
			assertEquals(AreaPosition(0, 1), areaState.getPlayerPosition(0))
			assertNull(areaState.suspension)

			testRendering(
				state, 1000, 800, "falling-star-2",
				roomColors, cutsceneColors,
			)

			// Walk to the door, and check that the mum-dialogue-cutscene doesn't replay
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(50) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			repeat(20) {
				state.update(updateContext)
			}

			areaState = state.campaign.state as AreaState
			assertEquals("gz_Mhouse1", areaState.area.properties.rawName)
			assertEquals(AreaPosition(0, 5), areaState.getPlayerPosition(0))
			assertNull(areaState.suspension)
		}
	}
}
