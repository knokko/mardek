package mardek.editor.client

import mardek.content.Content
import java.io.File
import java.util.Scanner
import java.util.UUID

class EditorClientProperties(val serverIp: String, val serverPort: Int, val password: UUID) {

	companion object {
		fun load(): EditorClientProperties {
			val scanner = Scanner(File("${Content.RESOURCES_DIRECTORY}/../editor-client/client.properties"))
			var serverIp = ""
			var serverPort = 0
			var password: UUID? = null
			while (scanner.hasNextLine()) {
				val line = scanner.nextLine()
				val separatorIndex = line.indexOf("=")
				if (separatorIndex == -1) continue

				val key = line.substring(0, separatorIndex)
				val value = line.substring(separatorIndex + 1)
				when (key) {
					"server-ip" -> serverIp = value
					"server-port" -> serverPort = Integer.parseInt(value)
					"password" -> password = UUID.fromString(value)
					else -> println("Warning: skipping unexpected property $line")
				}
			}
			scanner.close()

			if (serverIp == "" || serverPort == 0 || password == null) {
				throw RuntimeException("Missing required properties in client.properties")
			}

			return EditorClientProperties(serverIp, serverPort, password)
		}
	}
}
