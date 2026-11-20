package mardek.editor.client

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import mardek.content.BITSER
import mardek.editor.InitialClientConfig
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.nio.file.Files

@Composable
fun PasteSettingsApp() {
	Column {
		var error by remember { mutableStateOf("") }
		Row {
			Text(
				"Welcome to the MARDEK editor client",
				style = LocalTextStyle.current.copy(fontSize = 1.2.em, fontWeight = FontWeight.Bold)
			)
		}
		Row { Text("To activity it, copy the configuration (typically Control + C), and click the button below") }
		Row {
			Button(onClick = {
				println("test")
				val rawConfig = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor).toString()
				try {
					val hexBytes = rawConfig.hexToByteArray()
					val config = BITSER.fromBytes(InitialClientConfig::class.java, hexBytes)

					Files.write(certificateFile, config.serverCertificate)
					EditorClientProperties(
						serverIp = config.serverIp,
						serverPort = config.serverPort,
						password = config.initialPassword,
					).save(EditorClientProperties.FILE)
				} catch (_: IllegalArgumentException) {
					error = "Invalid configuration: it should be a hexadecimal string (containing only 0 to 9 and A to F)"
				} catch (_: Throwable) {
					error = "Failed to decode the configuration"
				}
			}) { Text("Activate") }
		}
		if (error.isNotBlank()) {
			Row { Text(error) }
		}
	}
}
