package mardek.game.action

import mardek.content.action.ActionTalk
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
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds

object TestSaucerActions {

	fun testPossession(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(
				content, InputManager(), SoundQueue(), 100.milliseconds
			)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Defeated Mugbert"
			)

			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "saucer" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(2, 5), Direction.Up, skipFadeIn = true,
			)
			state.campaign.state = areaState

			val baseSaucerColors = arrayOf(
				Color(217, 193, 0), // Rohoph
				Color(143, 166, 176), // Floor
				Color(42, 59, 73), // Wall
				Color(161, 255, 255), // Glowing stuff
			)
			val ambientSaucerColors = arrayOf(
				Color(41, 115, 0), // Rohoph
				Color(23, 98, 176), // Floor
				Color(3, 31, 73), // Wall
				Color(28, 153, 255), // Glowing stuff
			)
			val baseDialogueColors = arrayOf(
				Color(238, 203, 127),
				Color(73, 53, 38),
			)

			testRendering(
				state, 600, 400, "saucer1",
				baseSaucerColors, ambientSaucerColors + baseDialogueColors
			)

			// Trigger the possession action sequence
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))

			// Wait until the area ambience changes, and until the initial flash is gone
			repeat(20) {
				state.update(updateContext)
			}
			sleep(800)

			testRendering(
				state, 600, 400, "saucer2",
				ambientSaucerColors, baseSaucerColors + baseDialogueColors
			)

			assertNull(state.campaign.determineMusicTrack(content))

			repeat(25) {
				state.update(updateContext)
			}

			// Test that this doesn't crash
			dummySaveManager().createSave(
				content, state.campaign,
				"DoNotCrash", SaveFile.Type.Cheat,
			)

			// Wait until the possession dialogue starts, and until the second flash is gone
			repeat(50) {
				state.update(updateContext)
			}
			assertEquals("Rohoph", state.campaign.determineMusicTrack(content))
			sleep(800)

			testRendering(
				state, 600, 400, "saucer3",
				baseSaucerColors + baseDialogueColors, ambientSaucerColors
			)

			// Press the Interact key until Rohoph starts talking
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			repeat(10) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			repeat(10) {
				state.update(updateContext)
			}

			testRendering(
				state, 600, 400, "saucer4",
				baseSaucerColors + baseDialogueColors, ambientSaucerColors,
			)

			fun assertSpeaking(expectedName: String) {
				val node = (areaState.suspension as AreaSuspensionActions).actions.node as FixedActionNode
				val speaker = (node.action as ActionTalk).speaker
				assertEquals(expectedName, speaker.getDisplayName(null, emptyArray()))
			}
			assertSpeaking("????")

			// Skip the rest of the dialogue
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(50) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))

			assertNull(areaState.suspension)
			assertNull(state.campaign.determineMusicTrack(content))

			// Interact with the body of Rohoph
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			repeat(10) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertSpeaking("Rohoph")

			// Finish the 'dialogue', and exit the saucer
			updateContext.input.postEvent(pressKeyEvent(InputKey.Cancel))
			repeat(10) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Cancel))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(20) {
				state.update(updateContext)
			}

			// Check that we managed to exit the saucer
			val newAreaState = state.campaign.state as AreaState
			assertEquals("crashsite", newAreaState.area.properties.rawName)

			// Check that there is no Mugbert
			val mugbertCharacter = newAreaState.area.objects.characters[0]
			assertEquals("Mugbert", mugbertCharacter.name)
			assertNull(newAreaState.getCharacterState(mugbertCharacter))

			// Check that it is evening
			assertNotNull(state.campaign.story.evaluate(
				newAreaState.area.properties.ambience, state.campaign.expressionContext()
			))
			assertEquals("crickets", state.campaign.determineMusicTrack(content))
		}
	}
}
