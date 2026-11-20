package mardek.editor.client

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mardek.content.BITSER
import mardek.content.Content
import mardek.editor.InitialClientConfig
import java.io.File
import java.nio.file.Files

@Composable
fun PasteSettingsApp(onFinish: () -> Unit) {
	Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
		Row {
			Text(
				"Welcome to the MARDEK editor client",
				style = LocalTextStyle.current.copy(fontSize = 1.2.em, fontWeight = FontWeight.Bold)
			)
		}
		Row { Text("To activate it, copy & paste the authentication key below, and click \"Activate\"") }

		val textState = rememberTextFieldState()
		Row { OutlinedTextField(textState, modifier = Modifier.fillMaxWidth(0.95f).height(200.dp)) }

		var error = ""
		Row {
			var config: InitialClientConfig? = null
			try {
				val trimmedConfig = textState.text.toString()
					.replace("\n", "")
					.replace("\r", "")
					.replace("\t", "")
					.replace(" ", "")
				if (trimmedConfig.isNotBlank()) {
					val hexBytes = trimmedConfig.hexToByteArray()
					config = BITSER.fromBytes(InitialClientConfig::class.java, hexBytes)
				}
			} catch (_: IllegalArgumentException) {
				error = "Invalid configuration: it should be a hexadecimal string (even number of characters, containing only 0 to 9 and A to F)"
			} catch (failed: Throwable) {
				error = "Failed to decode the configuration"
				failed.printStackTrace()
			}

			Button(onClick = {
				val publicKeyFile = File(
					"${Content.RESOURCES_DIRECTORY}/../editor-server/certificate/public-certificate.pem"
				)
				publicKeyFile.parentFile.mkdirs()
				Files.write(publicKeyFile.toPath(), config!!.serverCertificate)

				EditorClientProperties(
					serverIp = config.serverIp,
					serverPort = config.serverPort,
					password = config.initialPassword,
				).save(EditorClientProperties.FILE)
				onFinish()
			}, enabled = config != null) { Text("Activate") }
		}

		if (error.isNotBlank()) {
			Row { Text(error) }
		}
	}
}
