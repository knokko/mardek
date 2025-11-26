package mardek.state

import mardek.state.saves.VIDEO_SETTINGS_FILE
import java.io.File
import java.io.PrintWriter
import java.lang.Integer.parseInt
import java.util.Scanner

class VideoSettings(
	var preferredDevice: Int,
	var capFps: Boolean,
	var showFps: Boolean,
	var framesInFlight: Int,
	var delayRendering: Boolean,
) {
	lateinit var availableDevices: Array<Any>
	var canUncapFps = false

	fun save(settingsFile: File = VIDEO_SETTINGS_FILE) {
		try {
			settingsFile.parentFile.mkdirs()
			val writer = PrintWriter(settingsFile)
			writer.println("device=$preferredDevice")
			writer.println("cap-fps=$capFps")
			writer.println("show-fps=$showFps")
			writer.println("frames-in-flight=$framesInFlight")
			writer.println("delay-rendering=$delayRendering")
			writer.flush()
			writer.close()
		} catch (failed: Throwable) {
			failed.printStackTrace()
		}
	}

	companion object {
		fun load(settingsFile: File = VIDEO_SETTINGS_FILE): VideoSettings {
			val settings = VideoSettings(
				preferredDevice = 0,
				capFps = true,
				showFps = false,
				framesInFlight = 1,
				delayRendering = true,
			)

			if (settingsFile.exists()) {
				try {
					val scanner = Scanner(settingsFile)
					while (scanner.hasNextLine()) {
						val nextLine = scanner.nextLine()
						if (nextLine.startsWith("device=")) {
							settings.preferredDevice = parseInt(nextLine.substring("device=".length))
						}
						if (nextLine.startsWith("cap-fps=")) {
							settings.capFps = nextLine.endsWith("true")
						}
						if (nextLine.startsWith("show-fps=")) {
							settings.showFps = nextLine.endsWith("true")
						}
						if (nextLine.startsWith("frames-in-flight=")) {
							settings.framesInFlight = parseInt(nextLine.substring("frames-in-flight=".length))
						}
						if (nextLine.startsWith("delay-rendering")) {
							settings.delayRendering = nextLine.endsWith("true")
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
