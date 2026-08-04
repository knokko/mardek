package mardek.game.ui

import mardek.game.TestingInstance
import mardek.game.pressKeyEvent
import mardek.game.releaseKeyEvent
import mardek.game.repeatKeyEvent
import mardek.game.testRendering
import mardek.input.InputKey
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.saves.SavesFolderManager
import mardek.state.title.MusicPlayerState
import mardek.state.title.TitleScreenState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import java.awt.Color
import java.lang.Thread.sleep
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds

object TestMusicPlayer {

	fun runTest(instance: TestingInstance) {
		instance.apply {
			val updateContext = GameStateUpdateContext(
				content, titleContent, InputManager(), SoundQueue(), 100.milliseconds,
				saves = SavesFolderManager(discoveredMusicDirectory = Files.createTempDirectory("").toFile())
			)
			var state: GameState = TitleScreenState()
			assertSame(state, state.update(updateContext))

			val titleScreenColors = arrayOf(
				Color(190, 144, 95), // MARDEK outline color
				Color(255, 204, 153), // Button outline color
			)
			val musicPlayerColors = arrayOf(
				Color(22, 13, 13), // Upper/lower bar
				Color(131, 81, 38), // Title color
				Color(208, 193, 142), // Arrow color and scroll bar color
				Color(56, 53, 72), // One of the disabled category music note icons
				Color(51, 51, 204), // Crystal pointer color
				Color(238, 203, 127), // Bottom text color
				Color(248, 232, 194), // Upper "Back" text color
			)
			val disabledBattleMusicNoteColors = arrayOf(Color(47, 12, 12))
			val activeBattleMusicNoteColors = arrayOf(Color(126, 1, 1))
			val selectedMusicTrackColors = arrayOf(Color(221, 238, 253))

			// Render title screen whose fade-in is NOT complete
			testRendering(
				state, 800, 600, "music-player0",
				emptyArray(), titleScreenColors,
				saves = updateContext.saves,
			)
			assertEquals("Theme", state.determineMusic(
				null, content.audio
			).track!!.fileName)

			// Await fade-in
			sleep(500)
			testRendering(
				state, 800, 600, "music-player1",
				titleScreenColors, emptyArray(),
				saves = updateContext.saves,
			)

			// Go to the Music Player
			updateContext.input.postEvent(MouseMoveEvent(
				(state as TitleScreenState).musicPlayerButton!!.minX, state.musicPlayerButton!!.minY
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			assertSame(state, state.update(updateContext))

			// Check fade-out of the Title Screen
			sleep(200)
			testRendering(
				state, 800, 600, "music-player2",
				emptyArray(), titleScreenColors,
				saves = updateContext.saves,
			)

			sleep(300)
			state = state.update(updateContext)
			assertInstanceOf<MusicPlayerState>(state)

			// music-player3 check was deleted because it was inherently flaky

			// Await fade-in of the Music Player
			sleep(470)
			testRendering(
				state, 800, 600, "music-player4",
				musicPlayerColors + disabledBattleMusicNoteColors,
				activeBattleMusicNoteColors + selectedMusicTrackColors,
				saves = updateContext.saves,
			)

			assertNull(state.determineMusic(null, content.audio).track)

			// Scroll to the Boss Battle track
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveRight))
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.MoveDown))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			updateContext.input.postEvent(releaseKeyEvent(InputKey.Interact))
			assertSame(state, state.update(updateContext))

			// Check that the boss battle track is greyed out, because it's not unlocked yet
			testRendering(
				state, 800, 600, "music-player5",
				musicPlayerColors + activeBattleMusicNoteColors,
				selectedMusicTrackColors, saves = updateContext.saves,
			)

			// Also check that we can NOT play it yet, because it's not unlocked yet
			assertNull(state.determineMusic(null, content.audio).track)

			// Unlock the boss battle track, play the track, and render again
			updateContext.saves.unlockMusicTrack(
				content.audio.musicTracks.find { it.fileName == "BossBattle" }!!
			)
			updateContext.input.postEvent(pressKeyEvent(InputKey.Interact))
			assertSame(state, state.update(updateContext))
			var musicJob = state.determineMusic(null, content.audio)
			assertEquals("BossBattle", musicJob.track!!.fileName)
			assertFalse(musicJob.shouldPause)

			testRendering(
				state, 800, 600, "music-player6",
				musicPlayerColors + activeBattleMusicNoteColors + selectedMusicTrackColors,
				emptyArray(), saves = updateContext.saves,
			)

			// Pause the boss battle track
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
			assertSame(state, state.update(updateContext))
			musicJob = state.determineMusic(null, content.audio)
			assertEquals("BossBattle", musicJob.track!!.fileName)
			assertTrue(musicJob.shouldPause)

			// Scroll to another track, and try to play it.
			// This should not DO anything, since we haven't unlocked the other track yet
			updateContext.input.postEvent(pressKeyEvent(InputKey.MoveUp))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Interact))
			assertSame(state, state.update(updateContext))
			musicJob = state.determineMusic(null, content.audio)
			assertEquals("BossBattle", musicJob.track!!.fileName)
			assertTrue(musicJob.shouldPause)

			testRendering(
				state, 800, 600, "music-player7",
				musicPlayerColors + activeBattleMusicNoteColors,
				selectedMusicTrackColors, saves = updateContext.saves,
			)

			// Click on the 'play' button to resume the boss battle track
			updateContext.input.postEvent(MouseMoveEvent(
				state.pauseButton!!.maxX, state.pauseButton!!.maxY
			))
			updateContext.input.postEvent(pressKeyEvent(InputKey.Click))
			assertSame(state, state.update(updateContext))
			musicJob = state.determineMusic(null, content.audio)
			assertEquals("BossBattle", musicJob.track!!.fileName)
			assertFalse(musicJob.shouldPause)

			// Click on the "Back" button to start the fade-out
			updateContext.input.postEvent(MouseMoveEvent(
				state.backButton!!.maxX, state.backButton!!.maxY
			))
			updateContext.input.postEvent(repeatKeyEvent(InputKey.Click))
			assertSame(state, state.update(updateContext))

			sleep(300)
			testRendering(
				state, 800, 600, "music-player8",
				emptyArray(), musicPlayerColors + titleScreenColors,
				saves = updateContext.saves,
			)

			sleep(300)
			state = state.update(updateContext)
			assertInstanceOf<TitleScreenState>(state)
		}
	}
}
