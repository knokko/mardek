package mardek.game.area

import mardek.content.action.ActionTalk
import mardek.content.action.FixedActionNode
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration.Companion.milliseconds

object TestTimelines {

	fun testHeroesDenTriggerTooLate(instance: TestingInstance) {
		instance.apply {
			val fakeInput = InputManager()
			val updateContext = GameStateUpdateContext(content, fakeInput, SoundQueue(), 10.milliseconds)
			val state = InGameState(simpleCampaignState(), "")

			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'"
			)

			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "heroes_den" }!!,
				state.campaign.story, AreaPosition(10, 6)
			)

			repeat(10) {
				state.update(updateContext)
			}
			assertNull((state.campaign.state as AreaState).suspension)
		}
	}

	fun testGeorgeBlockadeDuringChapter1Night(instance: TestingInstance) {
		instance.apply {
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			val state = InGameState(simpleCampaignState(), "")

			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'",
			)

			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				state.campaign.story, AreaPosition(34, 7)
			)
			state.campaign.state = areaState

			// Test that we cannot walk through George
			state.update(updateContext)
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			repeat(100) {
				state.update(updateContext)
			}
			assertEquals(7, areaState.getPlayerPosition(0).y)

			// Test that we can interact with George
			assertNull(areaState.suspension)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(3) {
				state.update(updateContext)
			}
			val actions = (areaState.suspension as AreaSuspensionActions).actions
			assertEquals(
				"You're out late there, boys! Shouldn't you be going back home to bed? " +
						"Your parents are probably worried!",
				((actions.node as FixedActionNode).action as ActionTalk).text,
			)
		}
	}

	fun testGeorgeIsGoneDuringChapter1Day(instance: TestingInstance) {
		instance.apply {
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			val state = InGameState(simpleCampaignState(), "")

			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Searching for the fallen 'star'",
			)

			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				state.campaign.story, AreaPosition(34, 7)
			)
			state.campaign.state = areaState

			// Test that we can NOT interact with George
			assertNull(areaState.suspension)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertNull(areaState.suspension)

			// Test that we can walk through not-existing George
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			repeat(100) {
				state.update(updateContext)
			}
			assertEquals(AreaPosition(34, 4), areaState.getPlayerPosition(0))
		}
	}

	fun testGeorgeBlockadeDuringLastChapter1Night(instance: TestingInstance) {
		instance.apply {
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			val state = InGameState(simpleCampaignState(), "")

			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "After the conversation in Rohophs saucer is finished",
			)

			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				state.campaign.story, AreaPosition(34, 7)
			)
			state.campaign.state = areaState

			// Test that we can interact with George, and that we get the right dialogue
			assertNull(areaState.suspension)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			repeat(3) {
				state.update(updateContext)
			}

			val actions = (areaState.suspension as AreaSuspensionActions).actions
			assertEquals(
				"Hello there, boys! Back from another adventure again? I'm just, uh... " +
						"just wandering the streets. Yes, there's nothing suspicious about THAT, is there?",
				((actions.node as FixedActionNode).action as ActionTalk).text,
			)
		}
	}
}
