package mardek.audio

import mardek.state.GameStateManager
import mardek.state.ingame.InGameState
import mardek.state.title.TitleScreenState

class AudioUpdater(private val stateManager: GameStateManager) {

	private val manager = AudioManager()

	private val musicMap = mutableMapOf<String, Int>()

	private val titleScreen = manager.add("TitleScreen.ogg")

	private val menuScroll = manager.add("7_sfx_menuBlip1.ogg")
	private val menuPartyScroll = manager.add("2_sfx_menuSwish.ogg")
	private val menuOpen = manager.add("3_sfx_menuOpen.ogg")
	private val clickConfirm = manager.add("4_sfx_menuBlip4.ogg")
	private val clickCancel = manager.add("5_sfx_menuBlip3.ogg")
	private val clickReject = manager.add("5400_sfx_error.ogg")
	private val openChest = manager.add("5411_sfx_Open1.ogg")
	private val toggleSkill = manager.add("6_sfx_menuBlip2.ogg")

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
			musicTrack = musicMap.computeIfAbsent(trackName!!) { track -> manager.add("$track.ogg") }
		}
		if (musicTrack != null) manager.playMusic(musicTrack!!)
		if (nextSound == "menu-scroll") manager.playSound(menuScroll)
		if (nextSound == "menu-party-scroll") manager.playSound(menuPartyScroll)
		if (nextSound == "menu-open") manager.playSound(menuOpen)
		if (nextSound == "click-confirm") manager.playSound(clickConfirm)
		if (nextSound == "click-cancel") manager.playSound(clickCancel)
		if (nextSound == "click-reject") manager.playSound(clickReject)
		if (nextSound == "open-chest") manager.playSound(openChest)
		if (nextSound == "toggle-skill") manager.playSound(toggleSkill)
	}

	fun destroy() = manager.destroy()
}
