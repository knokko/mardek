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
 * The file where the video settings will be stored
 */
val VIDEO_SETTINGS_FILE = File("$STORAGE_DIRECTORY/video-settings")

val LOGS_DIRECTORY = File("$STORAGE_DIRECTORY/logs")
