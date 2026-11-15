package mardek.editor.server

import mardek.content.Content
import java.io.File
import java.lang.Integer.parseInt
import java.util.Scanner

class EditorServerProperties(val ip: String, val port: Int, val password: String) {

	companion object {
		fun load(): EditorServerProperties {
			val scanner = Scanner(File("${Content.RESOURCES_DIRECTORY}/../editor-server/server.properties"))
			var ip = ""
			var port = 0
			var password = ""
			while (scanner.hasNextLine()) {
				val line = scanner.nextLine()
				val separatorIndex = line.indexOf("=")
				if (separatorIndex == -1) continue

				val key = line.substring(0, separatorIndex)
				val value = line.substring(separatorIndex + 1)
				when (key) {
					"ip" -> ip = value
					"port" -> port = parseInt(value)
					"password" -> password = value
					else -> println("Warning: skipping unexpected property $line")
				}
			}
			scanner.close()

			if (ip == "" || port == 0 || password == "") {
				throw RuntimeException("Missing required properties in server.properties")
			}

			return EditorServerProperties(ip, port, password)
		}
	}
}
