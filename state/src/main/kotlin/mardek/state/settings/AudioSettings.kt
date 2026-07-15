package mardek.state.settings

import mardek.state.saves.AUDIO_SETTINGS_FILE
import java.io.File
import java.io.PrintWriter
import java.lang.Integer.parseInt
import java.util.Scanner

/**
 * The *audio* settings that this engine supports (e.g. music/sound volume).
 *
 * - The auio settings are loaded from [mardek.state.saves.AUDIO_SETTINGS_FILE] when the game is launched,
 * if the file exists. Otherwise, the default audio settings are used.
 * - The loaded `AudioSettings` instance is stored in the `MardekWindow.userSettings.audioSettings` field.
 * - The `AudioSettings can be edited from the "Settings" -> "Audio" tab of the in-game menu.
 * When the player makes changes, the audio settings will be written to [mardek.state.saves.AUDIO_SETTINGS_FILE].
 */
class AudioSettings(

	/**
	 * The master volume, as percentage
	 */
	var masterVolume: Int,

	/**
	 * The volume% for the *music* only (so no other sound effects)
	 */
	var musicVolume: Int,

	/**
	 * The volume% for the *sound* effects (e.g. attack sounds and click sounds), but *not* the music.
	 */
	var soundEffectVolume: Int,
) {

	/**
	 * Saves the audio settings to disk (to [mardek.state.saves.AUDIO_SETTINGS_FILE] by default,
	 * but some unit tests use a different location).
	 */
	fun save(settingsFile: File = AUDIO_SETTINGS_FILE) {
		try {
			settingsFile.parentFile.mkdirs()
			val writer = PrintWriter(settingsFile)
			writer.println("master=$masterVolume")
			writer.println("music=$musicVolume")
			writer.println("sounds=$soundEffectVolume")
			writer.flush()
			writer.close()
		} catch (failed: Throwable) {
			failed.printStackTrace()
		}
	}

	companion object {

		/**
		 * Loads the audio settings from disk (from [AUDIO_SETTINGS_FILE] by default, but some unit tests use a
		 * different file).
		 */
		fun load(settingsFile: File = AUDIO_SETTINGS_FILE): AudioSettings {
			val settings = AudioSettings(
				masterVolume = 50,
				musicVolume = 100,
				soundEffectVolume = 100,
			)

			if (settingsFile.exists()) {
				try {
					val scanner = Scanner(settingsFile)
					while (scanner.hasNextLine()) {
						val nextLine = scanner.nextLine()
						if (nextLine.startsWith("master=")) {
							settings.masterVolume = parseInt(nextLine.substring("master=".length))
						}
						if (nextLine.startsWith("music=")) {
							settings.musicVolume = parseInt(nextLine.substring("music=".length))
						}
						if (nextLine.startsWith("sounds=")) {
							settings.soundEffectVolume = parseInt(nextLine.substring("sounds=".length))
						}
					}
					scanner.close()
				} catch (failed: Throwable) {
					failed.printStackTrace()
				}
			}

			return settings
		}
	}
}
