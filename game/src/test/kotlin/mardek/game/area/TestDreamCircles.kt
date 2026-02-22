package mardek.game.area

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.time.Duration.Companion.milliseconds

object TestDreamCircles {

	fun testDreamCircleInMagicShopChapter1(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "dream circles")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			performTimelineTransition(
				updateContext, state.campaign, "MainTimeline",
				"Searching for the fallen 'star'"
			)
			val areaState = AreaState(
				content.areas.areas.find { it.properties.rawName == "gz_shop_M" }!!,
				state.campaign.story, AreaPosition(7, 3)
			)
			state.campaign.state = areaState

			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			repeat(200) {
				state.update(updateContext)
			}
			assertEquals(AreaPosition(7, 1), areaState.getPlayerPosition(0))
			assertTrue(areaState.area.objects.portals.find { it.x == 7 && it.y == 2 } != null)
			assertSame(areaState, state.campaign.state)
		}
	}
}
