package mardek.audio

import mardek.content.Content
import mardek.content.audio.AudioContent
import mardek.content.audio.MusicTrack
import mardek.content.audio.SoundEffect
import mardek.state.GameStateManager
import mardek.state.settings.AudioSettings
import java.util.concurrent.CompletableFuture
import kotlin.time.DurationUnit

/**
 * The `AudioUpdater` makes sure that the sounds in the `SoundQueue` are actually drained & played.
 * Furthermore, it ensures that the right music is played at the right time.
 *
 * The `MardekWindow` class will repeatedly invoke the `update()` method of this class while the game is running, and
 * invokes the `destroy()` method when the game is closed.
 */
class AudioUpdater(
	private val stateManager: GameStateManager,
	private val getContent: CompletableFuture<Content>,
	private val audioContent: AudioContent,
	private val audioSettings: AudioSettings,
) {
	private val manager = AudioManager()

	private val musicMap = mutableMapOf<String, Int>()
	private val soundMap = mutableMapOf<SoundEffect, Int>()

	/**
	 * This method should be called repeatedly as long as the game is running. The higher the update frequency, the
	 * better the sound/music timing.
	 */
	fun update() {
		val nextSound = stateManager.soundQueue.take()
		var musicTrack: MusicTrack? = null
		var rawMusicTrack: Int? = null

		val expectedMusicVolume: Float
		val expectedSoundsVolume: Float
		synchronized(stateManager.lock()) {
			val state = stateManager.currentState
			val content = if (getContent.isDone) getContent.get() else null
			musicTrack = state.determineMusicTrack(content, audioContent)

			val masterVolume = audioSettings.masterVolume * 0.01f
			expectedMusicVolume = masterVolume * audioSettings.musicVolume * 0.01f
			expectedSoundsVolume = masterVolume * audioSettings.soundEffectVolume * 0.01f
		}

		manager.useMusicVolume(expectedMusicVolume)
		manager.useSoundsVolume(expectedSoundsVolume)

		if (musicTrack != null) {
			rawMusicTrack = musicMap.computeIfAbsent(musicTrack.fileName) {
				track -> manager.add("$track.ogg", null)
			}
		}
		if (rawMusicTrack != null) {
			val loopSeconds = musicTrack!!.loopAfter.toDouble(DurationUnit.SECONDS)
			manager.playMusic(rawMusicTrack, loopSeconds.toFloat())
		}
		else manager.stopMusic()

		if (nextSound != null) {
			val soundHandle = soundMap.computeIfAbsent(nextSound) {
				soundEffect -> manager.add("", soundEffect.oggData)
			}
			manager.playSound(soundHandle)
		}
	}

	/**
	 * Destroys the audio subsystem. This should be called when the game is shutting down.
	 */
	fun destroy() = manager.destroy()
}
