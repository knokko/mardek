package mardek.state.saves

import java.io.File

val STORAGE_DIRECTORY = File("${System.getProperty("user.home")}/MARDEK")
val SAVES_DIRECTORY = File("$STORAGE_DIRECTORY/saves")
val VIDEO_SETTINGS_FILE = File("$STORAGE_DIRECTORY/video-settings")
