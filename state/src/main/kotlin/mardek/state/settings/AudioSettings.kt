package mardek.state.settings

import mardek.state.saves.AUDIO_SETTINGS_FILE
import java.io.File
import java.io.PrintWriter
import java.lang.Boolean.parseBoolean
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

	/**
	 * When a playable character masters an active skill or a passive skill,
	 * the game will play the mastery jingle sound effect.
	 *
	 * This field determines whether we also play the mastery jingle when
	 * a playable character masters a *reaction* skill.
	 *
	 * This is `false` by default, since vanilla MARDEK doesn't do this.
	 * However, I added this setting because I personally find it convenient, but not everyone agrees.
	 */
	var playReactionMasteryJingle: Boolean,
) {

	/**
	 * Saves the audio settings to disk (to [mardek.state.saves.AUDIO_SETTINGS_FILE] by default,
	 * but some unit tests use a different location).
	 */
	fun save(settingsFile: File = AUDIO_SETTINGS_FILE) {
		try {
			settingsFile.parentFile.mkdirs()
			val writer = PrintWriter(settingsFile)
			writer.println("${Keys.MASTER}$masterVolume")
			writer.println("${Keys.MUSIC}$musicVolume")
			writer.println("${Keys.SOUNDS}$soundEffectVolume")
			writer.println("${Keys.MASTERY_REACTION_JINGLE}$playReactionMasteryJingle")
			writer.flush()
			writer.close()
		} catch (failed: Throwable) {
			failed.printStackTrace()
		}
	}

	companion object {

		internal fun defaultSettings() = AudioSettings(
			masterVolume = 50,
			musicVolume = 100,
			soundEffectVolume = 100,
			playReactionMasteryJingle = false,
		)

		private object Keys {

			const val MASTER = "master="
			const val MUSIC = "music="
			const val SOUNDS = "sounds="
			const val MASTERY_REACTION_JINGLE = "play-reaction-mastery-jingle="
		}

		/**
		 * Loads the audio settings from disk (from [AUDIO_SETTINGS_FILE] by default, but some unit tests use a
		 * different file).
		 */
		fun load(settingsFile: File = AUDIO_SETTINGS_FILE): AudioSettings {
			val settings = defaultSettings()

			if (settingsFile.exists()) {
				try {
					val scanner = Scanner(settingsFile)
					while (scanner.hasNextLine()) {
						val nextLine = scanner.nextLine()
						if (nextLine.startsWith(Keys.MASTER)) {
							settings.masterVolume = parseInt(nextLine.substring(Keys.MASTER.length))
						}
						if (nextLine.startsWith(Keys.MUSIC)) {
							settings.musicVolume = parseInt(nextLine.substring(Keys.MUSIC.length))
						}
						if (nextLine.startsWith(Keys.SOUNDS)) {
							settings.soundEffectVolume = parseInt(nextLine.substring(Keys.SOUNDS.length))
						}
						if (nextLine.startsWith(Keys.MASTERY_REACTION_JINGLE)) {
							settings.playReactionMasteryJingle = parseBoolean(
								nextLine.substring(Keys.MASTERY_REACTION_JINGLE.length)
							)
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
