package mardek.game.ui

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
import mardek.state.ingame.worldmap.WorldMapState
import mardek.state.saves.SaveFile
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

object TestWorldMap {

	fun testBeforeSoothwood(instance: TestingInstance) {
		instance.apply {
			val saves = dummySaveManager()
			val state = InGameState(simpleCampaignState(), "")
			val updateContext = GameStateUpdateContext(content, InputManager(), SoundQueue(), 10.milliseconds)
			state.campaign.state = AreaState(
				area = content.areas.areas.find { it.properties.rawName == "heroes_den" }!!,
				initialPlayerPosition = AreaPosition(6, 13),
				initialPlayerDirection = Direction.Up,
			)
			state.campaign.characterStates[heroMardek]!!.currentLevel = 50
			state.campaign.characterStates[heroDeugan]!!.currentLevel = 50
			performTimelineTransition(
				updateContext, state.campaign,
				"MainTimeline", "Night before the falling 'star'"
			)

			val titleBarColor = arrayOf(Color(73, 59, 50))
			val heroesDenColors = arrayOf(
				Color(71, 74, 26), // Trees
				Color(104, 103, 42), // Grass
				Color(102, 38, 0), // Mardek hair
			)
			val worldMapColors = arrayOf(
				Color(185, 168, 130), // Water & paths
				Color(164, 138, 96), // Land
				Color(102, 50, 0), // Mardek hair
				Color(247, 0, 0), // Available node
				Color(104, 88, 56), // Path borders
			)
			val activatedNodeColor = arrayOf(Color(242, 132, 0))
			// Await fade-in
			repeat(100) {
				state.update(updateContext)
			}
			assertInstanceOf<AreaState>(state.campaign.state)
			testRendering(
				state, 600, 400, "world-map0",
				heroesDenColors + titleBarColor, worldMapColors,
			)

			// Saving should not crash
			saves.createSave(content, state.campaign, "test", SaveFile.Type.Cheat)

			// Move down to exit Heroes's den, which should start the area fade-out
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(12) {
				state.update(updateContext)
			}
			assertInstanceOf<AreaState>(state.campaign.state)
			testRendering(
				state, 600, 400, "world-map1",
				titleBarColor, heroesDenColors + worldMapColors,
			)
			repeat(12) {
				state.update(updateContext)
			}
			assertInstanceOf<WorldMapState>(state.campaign.state)
			testRendering(
				state, 600, 400, "world-map2",
				titleBarColor, heroesDenColors + worldMapColors,
			)

			// Saving should not crash
			saves.createSave(content, state.campaign, "test", SaveFile.Type.Cheat)

			// Await the world map fade-in
			repeat(50) {
				state.update(updateContext)
			}
			testRendering(
				state, 600, 400, "world-map3",
				titleBarColor + worldMapColors + activatedNodeColor, heroesDenColors,
			)

			// Walking down or left should be impossible
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)

			val mapState = state.campaign.state as WorldMapState
			assertNull(mapState.nextNode)
			assertNull(updateContext.soundQueue.take())

			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			state.update(updateContext)
			assertNull(mapState.nextNode)
			assertNull(updateContext.soundQueue.take())

			// Walking up to Goznor should be possible
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			state.update(updateContext)
			assertNotNull(mapState.nextNode)
			assertNull(updateContext.soundQueue.take())
			testRendering(
				state, 600, 400, "world-map4",
				worldMapColors, activatedNodeColor,
			)

			// Saving should not crash
			saves.createSave(content, state.campaign, "test", SaveFile.Type.Cheat)

			// Pressing other keys should not have any effect
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))

			// Wait until the player reaches Goznor
			repeat(500) {
				state.update(updateContext)
			}
			assertNull(mapState.nextNode)
			assertSame(content.audio.fixedEffects.ui.toggleSkill, updateContext.soundQueue.take())
			assertNull(updateContext.soundQueue.take())
			assertSame(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				mapState.currentNode.entrances[0].area,
			)
			testRendering(
				state, 600, 400, "world-map5",
				worldMapColors + activatedNodeColor, emptyArray(),
			)

			// Walking up or right should be impossible
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.update(updateContext)
			assertNull(mapState.nextNode)

			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			state.update(updateContext)
			assertNull(mapState.nextNode)

			// Walking left should take the player back to Heroes' Den, since Soothwood is not discovered yet
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveLeft))
			state.update(updateContext)
			assertNotNull(mapState.nextNode)
			repeat(500) {
				state.update(updateContext)
			}
			assertNull(mapState.nextNode)
			assertSame(
				content.areas.areas.find { it.properties.rawName == "heroes_den" }!!,
				mapState.currentNode.entrances[0].area,
			)

			// Walk to Goznor again, by walking right
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveLeft))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			state.update(updateContext)
			assertNotNull(mapState.nextNode)
			repeat(500) {
				state.update(updateContext)
			}
			assertNull(mapState.nextNode)
			assertSame(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				mapState.currentNode.entrances[0].area,
			)

			// Finally enter Goznor
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			state.update(updateContext)
			assertNotNull(mapState.exiting)

			// Saving should not crash
			saves.createSave(content, state.campaign, "test", SaveFile.Type.Cheat)

			// Pressing any more keys should not have any effect
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			repeat(45) {
				state.update(updateContext)
				assertNull(mapState.nextNode)
				assertNotNull(mapState.exiting)
			}
			testRendering(
				state, 600, 400, "world-map6",
				titleBarColor, worldMapColors, // Should fade out
			)

			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			repeat(150) {
				state.update(updateContext)
			}
			assertInstanceOf<AreaState>(state.campaign.state)
			assertSame(
				content.areas.areas.find { it.properties.rawName == "goznor" }!!,
				(state.campaign.state as AreaState).area,
			)
			testRendering(
				state, 600, 400, "world-map7",
				heroesDenColors, worldMapColors,
			)
		}
	}

	// TODO CHAP1 Test Goznor to Soothwood on world map
	// TODO CHAP1 Test Crash site to Soothwood on world map
	// TODO CHAP1 Test Soothwood to Soothwood on world map, from both entrances
}
