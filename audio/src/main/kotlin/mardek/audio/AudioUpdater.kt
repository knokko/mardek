package mardek.audio

import mardek.content.audio.SoundEffect
import mardek.state.GameStateManager
import mardek.state.ingame.InGameState
import mardek.state.title.TitleScreenState

class AudioUpdater(private val stateManager: GameStateManager) {

	private val manager = AudioManager()

	private val musicMap = mutableMapOf<String, Int>()
	private val soundMap = mutableMapOf<SoundEffect, Int>()

	private val titleScreen = manager.add("TitleScreen.ogg", null)

	fun update() {
		val nextSound = stateManager.soundQueue.take()
		var trackName: String? = null
		var musicTrack: Int? = null

		synchronized(stateManager.lock()) {
			val state = stateManager.currentState

			if (state is TitleScreenState) musicTrack = titleScreen
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

	fun destroy() = manager.destroy()
}
