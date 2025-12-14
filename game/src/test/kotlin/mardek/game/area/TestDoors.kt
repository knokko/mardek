package mardek.game.area

import mardek.content.area.Direction
import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
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
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestDoors {

	fun testFireTemple(instance: TestingInstance) {
		instance.apply {
			val hub = content.areas.areas.find { it.properties.rawName == "Temple_FIRE_hub" }!!
			val state = InGameState(CampaignState(), "test")
			state.campaign.currentArea = AreaState(
				hub, AreaPosition(17, 12), Direction.Up
			)

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

	fun testDragonLair(instance: TestingInstance) {
		instance.apply {
			val state = InGameState(simpleCampaignState(), "test")
			state.campaign.currentArea = AreaState(dragonLairEntry, AreaPosition(5, 8))

			val doorColor = Color(60, 38, 27)
			val hairColorDeugan = Color(195, 156, 77)
			val expectedEntryColors = arrayOf(
				Color(59, 53, 68), // color between floor tiles
				Color(91, 79, 106), // light color of floor tiles
				Color(0, 0, 0), // black for the background
				Color(86, 50, 86), // color of braziers
				Color(101, 50, 0), // hair color of Mardek
				Color(70, 117, 33), // cape color of Deugan
				Color(96, 199, 242), // the save crystal
			)

			testRendering(
				state, 1000, 800, "dragon-lair-entry1",
				expectedEntryColors + doorColor, arrayOf(hairColorDeugan)
			)

			val dummySoundQueue = SoundQueue()
			val fakeInput = InputManager()
			fakeInput.postEvent(pressKeyEvent(InputKey.MoveUp))

			val context = GameStateUpdateContext(content, fakeInput, dummySoundQueue, 10.milliseconds)
			repeat(5000) {
				state.update(context)
			}

			testRendering(
				state, 1000, 800, "dragon-lair-entry2",
				expectedEntryColors + hairColorDeugan, emptyArray()
			)

			// Start opening the door
			fakeInput.postEvent(releaseKeyEvent(InputKey.MoveUp))
			fakeInput.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(context)

			// Once the door is being opened, pressing the left arrow key should NOT do anything,
			// since the player is not allowed to walk while opening a door
			fakeInput.postEvent(releaseKeyEvent(InputKey.Interact))
			fakeInput.postEvent(pressKeyEvent(InputKey.MoveLeft))

			repeat(2000) {
				val areaState = state.campaign.currentArea!!
				if (areaState.area.properties.rawName == "DL_entr") {
					assertEquals(AreaPosition(5, 2), areaState.getPlayerPosition(0))
					assertEquals(Direction.Up, areaState.getPlayerDirection(0))
				} else {
					assertEquals(AreaPosition(7, 39), areaState.getPlayerPosition(0))
				}

				state.update(context)
			}

			val expectedRoomColors = arrayOf(
				Color(59, 53, 68), // color between floor tiles
				Color(91, 79, 106), // light color of floor tiles
				Color(0, 0, 0), // black for the background
				Color(13, 0, 22), // dark blue for the rest of the background
				Color(101, 50, 0), // hair color of Mardek
			)
			testRendering(
				state, 1000, 800, "dragon-lair-room2",
				expectedRoomColors, arrayOf(hairColorDeugan)
			)
		}
	}
}
