package mardek.audio

import mardek.state.GameStateManager
import mardek.state.ingame.InGameState
import mardek.state.title.TitleScreenState

class AudioUpdater(private val stateManager: GameStateManager) {

	private val manager = AudioManager()

	private val musicMap = mutableMapOf<String, Int>()

	private val titleScreen = manager.add("TitleScreen.ogg")

	private val menuScroll = manager.add("7_sfx_menuBlip1.ogg")
	private val menuOpen = manager.add("3_sfx_menuOpen.ogg")
	private val clickConfirm = manager.add("4_sfx_menuBlip4.ogg")
	private val clickCancel = manager.add("5_sfx_menuBlip3.ogg")

	fun update() {
		val state = stateManager.currentState

		val nextSound = stateManager.soundQueue.take()
		if (nextSound == "menu-scroll") manager.playSound(menuScroll)
		if (nextSound == "menu-open") manager.playSound(menuOpen)
		if (nextSound == "click-confirm") manager.playSound(clickConfirm)
		if (nextSound == "click-cancel") manager.playSound(clickCancel)

		if (state is TitleScreenState) manager.playMusic(titleScreen)

		if (state is InGameState) {
			val area = state.campaign.currentArea?.area

			if (area != null) {
				val musicTrack = area.properties.musicTrack
				if (musicTrack != null) {
					val audio = musicMap.computeIfAbsent(musicTrack) { track ->
						manager.add("$track.ogg")
					}
					manager.playMusic(audio)
				}
			}
		}
	}

	fun destroy() = manager.destroy()
}
