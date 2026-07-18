package mardek.state.saves

import java.io.File

/**
 * The 'root' storage directory that this game will use to store saves and user settings/preferences
 */
val STORAGE_DIRECTORY = File("${System.getProperty("user.home")}/MARDEK")

/**
 * The directory that indirectly contains all the saves. All *child* directories of `SAVES_DIRECTORY` will contain
 * actual save files. The `SAVES_DIRECTORY` will contain 1 child directory per campaign. When a user starts a
 * *New Game*, he has to choose a name, which will be the name of the campaign/child directory.
 */
val SAVES_DIRECTORY = File("$STORAGE_DIRECTORY/saves")

/**
 * The directory that contains the IDs of the all the [mardek.content.audio.MusicTrack]s that have been 'unlocked'.
 * Only these music tracks can be played in the Music Player.
 *
 * Music tracks are marked as 'unlocked' by creating an (empty) file in this directory,
 * whose name is the id of the discovered music track.
 *
 * Note that unlike most other state, the discovered music tracks are *not* tied to a particular save file:
 * players can listen to all music tracks discovered during at least one save file.
 */
val UNLOCKED_MUSIC_DIRECTORY = File("$STORAGE_DIRECTORY/unlocked-music")

/**
 * The file where the video settings will be stored
 */
val VIDEO_SETTINGS_FILE = File("$STORAGE_DIRECTORY/video-settings")

/**
 * The file where the audio settings will be stored
 */
val AUDIO_SETTINGS_FILE = File("$STORAGE_DIRECTORY/audio-settings")

/**
 * The directory where all the game logs should be stored (potentially useful for debugging).
 *
 * Note that logs are only created when the `enable-logging` command-line argument is given.
 */
val LOGS_DIRECTORY = File("$STORAGE_DIRECTORY/logs")
