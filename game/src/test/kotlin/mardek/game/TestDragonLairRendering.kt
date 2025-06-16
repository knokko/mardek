package mardek.game

import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.renderer.SharedResources
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.InGameState
import mardek.state.ingame.area.AreaPosition
import mardek.state.ingame.area.AreaState
import java.awt.Color
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

fun testDragonLairDoor(instance: TestingInstance) {
	instance.apply {
		val getResources = CompletableFuture<SharedResources>()
		getResources.complete(SharedResources(getBoiler, 1, skipWindow = true))
		val state = InGameState(CampaignState(
			currentArea = AreaState(dragonLairEntry, AreaPosition(5, 8)),
			characterSelection = simpleCharacterSelectionState(),
			characterStates = simpleCharacterStates(),
			gold = 123
		))

		val doorColor = Color(59, 34, 22)
		val hairColorDeugan = Color(195, 156, 77)
		val expectedEntryColors = arrayOf(
			Color(59, 53, 66), // color between floor tiles
			Color(91, 79, 106), // light color of floor tiles
			Color(0, 0, 0), // black for the background
			Color(86, 50, 86), // color of braziers
			Color(101, 50, 0), // hair color of Mardek
			Color(69, 117, 28), // cape color of Deugan
			Color(96, 199, 242), // the save crystal
		)

		testRendering(
			getResources, state, 1000, 800, "dragon-lair-entry1",
			expectedEntryColors + doorColor, arrayOf(hairColorDeugan)
		)

		val dummySoundQueue = SoundQueue()
		val fakeInput = InputManager()
		fakeInput.postEvent(InputKeyEvent(InputKey.MoveUp, didPress = true, didRelease = false, didRepeat = false))

		val context = GameStateUpdateContext(content, fakeInput, dummySoundQueue, 10.milliseconds)
		for (counter in 0 until 5000) {
			state.update(context)
		}

		testRendering(
			getResources, state, 1000, 800, "dragon-lair-entry2",
			expectedEntryColors + hairColorDeugan, emptyArray()
		)

		fakeInput.postEvent(InputKeyEvent(InputKey.MoveUp, didPress = false, didRelease = true, didRepeat = false))
		fakeInput.postEvent(InputKeyEvent(InputKey.Interact, didPress = true, didRelease = false, didRepeat = false))

		for (counter in 0 until 2000) {
			state.update(context)
		}

		val expectedRoomColors = arrayOf(
			Color(59, 53, 66), // color between floor tiles
			Color(91, 79, 106), // light color of floor tiles
			Color(0, 0, 0), // black for the background
			Color(0, 0, 13), // dark blue for the rest of the background
			Color(101, 50, 0), // hair color of Mardek
		)
		testRendering(
			getResources, state, 1000, 800, "dragon-lair-room2",
			expectedRoomColors, arrayOf(hairColorDeugan)
		)

		getResources.get().destroy()
	}
}
