package mardek.game.area

import mardek.content.area.Direction
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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import kotlin.time.Duration.Companion.milliseconds

object TestTalkTriggers {

	fun testGoznorAccessoryShop(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "talk triggers")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			performTimelineTransition(
				updateContext, state.campaign, "MainTimeline",
				"Searching for the fallen 'star'",
			)

			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_shop_Ac" }!!,
				state.campaign.story, state.campaign.expressionContext(),
				AreaPosition(2, 3), Direction.Up,
			)
			state.campaign.state = areaState

			state.update(updateContext)
			assertNull(areaState.suspension)

			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertInstanceOf<AreaSuspensionActions>(areaState.suspension)

			val encyclopedia = state.campaign.encyclopedia.createSnapshot(content.encyclopedia, state.campaign)
			assertSame(
				content.encyclopedia.people.find { it.snapshots[0].firstName == "Social Fox" }!!.snapshots[0],
				encyclopedia.people.find { it.entry != null }!!.entry,
			)
			// TODO CHAP2 Test that the shop opens instead of the dialogue
		}
	}
}
