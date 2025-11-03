package mardek.audio

import mardek.content.audio.SoundEffect
import mardek.state.GameStateManager
import mardek.state.ingame.InGameState
import mardek.state.title.GameOverState
import mardek.state.title.TitleScreenState

/**
 * The `AudioUpdater` makes sure that the sounds in the `SoundQueue` are actually drained & played.
 * Furthermore, it ensures that the right music is played at the right time.
 *
 * The `MardekWindow` class will repeatedly invoke the `update()` method of this class while the game is running, and
 * invokes the `destroy()` method when the game is closed.
 */
class AudioUpdater(private val stateManager: GameStateManager) {

	private val manager = AudioManager()

	private val musicMap = mutableMapOf<String, Int>()
	private val soundMap = mutableMapOf<SoundEffect, Int>()

	private val titleScreen = manager.add("TitleScreen.ogg", null)
	private val gameOver = manager.add("GameOver.ogg", null)

	/**
	 * This method should be called repeatedly as long as the game is running. The higher the update frequency, the
	 * better the sound/music timing.
	 */
	fun update() {
		val nextSound = stateManager.soundQueue.take()
		var trackName: String? = null
		var musicTrack: Int? = null

		synchronized(stateManager.lock()) {
			val state = stateManager.currentState

			if (state is TitleScreenState) musicTrack = titleScreen
			if (state is GameOverState) musicTrack = gameOver
			if (state is InGameState) {
				val area = state.campaign.currentArea?.area
				if (area != null) trackName = area.properties.musicTrack
			}
		}

		if (trackName != null) {
			musicTrack = musicMap.computeIfAbsent(trackName) { track -> manager.add("$track.ogg", null) }
		}
		if (musicTrack != null) manager.playMusic(musicTrack)

		if (nextSound != null) {
			val soundHandle = soundMap.computeIfAbsent(nextSound) { soundEffect -> manager.add("", soundEffect.oggData) }
			manager.playSound(soundHandle)
		}
	}

	/**
	 * Destroys the audio subsystem. This should be called when the game is shutting down.
	 */
	fun destroy() = manager.destroy()
}
