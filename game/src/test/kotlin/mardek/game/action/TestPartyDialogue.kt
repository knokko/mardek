package mardek.game.action

import mardek.content.action.ActionTalk
import mardek.content.action.ChoiceActionNode
import mardek.content.action.FixedActionNode
import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.input.InputKey
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.area.AreaSuspensionActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration.Companion.milliseconds

object TestPartyDialogue {

	fun runTest(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = createUpdateContext(100.milliseconds)
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'"
			)
			state.campaign.state = AreaState(
				content.areas.areas.find { it.properties.rawName == "heroes_den" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(1, 3), Direction.Up,
			)

			updateContext.input.postEvent(pressKeyEvent(InputKey.PartyDialogue))
			state.update(updateContext)

			val actions = ((state.campaign.state as AreaState).suspension as AreaSuspensionActions).actions
			val initialNode = actions.node as FixedActionNode
			val initialAction = initialNode.action as ActionTalk
			assertEquals("Well, who should I talk to?", initialAction.text)

			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)

			assertInstanceOf<ChoiceActionNode>(actions.node)
			assertEquals(2, actions.choiceOptions.size)
			assertEquals("Talk to Deugan", actions.choiceOptions[0].text)
			assertEquals("Never mind...", actions.choiceOptions[1].text)

			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			val deuganNode = actions.node as FixedActionNode
			val deuganAction = deuganNode.action as ActionTalk
			assertEquals("Let's go home, Mardek.", deuganAction.text)

			// Test that saving doesn't crash
			createDummySave(updateContext.saves, "test-party-dialogue")

			// Skip it
			repeat(20) {
				state.update(updateContext)
			}
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertNull((state.campaign.state as AreaState).suspension)
		}
	}
}
