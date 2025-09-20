package mardek.state.saves

import java.io.File
import javax.swing.filechooser.FileSystemView

val STORAGE_DIRECTORY = File("${FileSystemView.getFileSystemView().homeDirectory}/MARDEK")
val SAVES_DIRECTORY = File("$STORAGE_DIRECTORY/saves")
val VIDEO_SETTINGS_FILE = File("$STORAGE_DIRECTORY/video-settings")
