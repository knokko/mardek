package mardek.game.area

import mardek.game.TestingInstance
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
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
				AreaPosition(10, 6)
			)

			repeat(10) {
				state.update(updateContext)
			}
			assertNull((state.campaign.state as AreaState).suspension)
		}
	}
}
