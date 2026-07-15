package mardek.state.settings

/**
 * This class contains all the user settings (video settings & audio settings).
 * More settings may be added in the future.
 *
 * The settings are saved on the computer of the user that is playing the game.
 * - The settings are loaded from disk when the game is launched, if the files exist.
 * Otherwise, the default settings are used.
 * - The loaded `UserSettings` instance is stored in the `MardekWindow.userSettings` field.
 * - The `UserSettings can be edited from the "Settings" tab of the in-game menu. When the player makes changes,
 * the settings will be saved to disk (in e.g. [mardek.state.saves.VIDEO_SETTINGS_FILE])
 */
class UserSettings(
	/**
	 * The *video* settings (e.g. FPS cap and preferred graphics card)
	 */
	val videoSettings: VideoSettings,

	/**
	 * The *audio* settings (e.g. the master volume)
	 */
	val audioSettings: AudioSettings,
) {

	/**
	 * Saves all settings to disk, to their default location (e.g. [mardek.state.saves.VIDEO_SETTINGS_FILE])
	 */
	fun save() {
		videoSettings.save()
		audioSettings.save()
	}

	companion object {

		/**
		 * Loads all settings from disk, from their default location (e.g. [mardek.state.saves.AUDIO_SETTINGS_FILE])
		 */
		fun load() = UserSettings(
			videoSettings = VideoSettings.load(),
			audioSettings = AudioSettings.load(),
		)
	}
}
