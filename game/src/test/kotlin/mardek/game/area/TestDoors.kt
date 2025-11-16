package mardek.game.area

import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration.Companion.seconds

object TestDoors {

	fun testFireTemple(instance: TestingInstance) {
		instance.apply {
			val hub = content.areas.areas.find { it.properties.rawName == "Temple_FIRE_hub" }!!
			val state = InGameState(CampaignState(
				currentArea = AreaState(
					hub, AreaPosition(17, 12), Direction.Up
				),
				characterSelection = simpleCharacterSelectionState(),
				characterStates = simpleCharacterStates(),
				gold = 123
			), "test")

			assertEquals(Direction.Up, state.campaign.currentArea!!.getPlayerDirection(0))

			val context = GameStateUpdateContext(content, InputManager(), SoundQueue(), 2.seconds)
			context.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(context)
			state.update(context)

			val newAreaState = state.campaign.currentArea!!
			println("new area state is $newAreaState")
			assertEquals("Temple_FIRE_SE", newAreaState.area.properties.rawName)
			assertEquals(AreaPosition(3, 3), newAreaState.getPlayerPosition(0))
			assertEquals(Direction.Down, newAreaState.getPlayerDirection(0))
		}
	}
}
