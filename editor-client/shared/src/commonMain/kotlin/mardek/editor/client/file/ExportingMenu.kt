package mardek.editor.client.file

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientStruct
import kotlinx.coroutines.launch
import mardek.exporting.ContentWrapper

@Composable
fun ExportingMenu(wrappedContentStruct: BitClientStruct) {
	Row {
		Text("Export/test the game...", style = LocalTextStyle.current.copy(fontSize = 1.5.em, fontWeight = FontWeight.Bold))
	}

	var wrappedContent by remember { mutableStateOf<ContentWrapper?>(null) }
	var lastSync by remember { mutableStateOf<Long?>(null) }

	val mutateScope = rememberCoroutineScope()
	Row {
		Button(
			onClick = { mutateScope.launch {
				lastSync = System.nanoTime()
			} },
			content = { Text("Synchronize") }
		)

		lastSync?.let { Text("Last sync: $it") }
	}

	if (wrappedContent == null) {
		Row { Text("You need to synchronize before you can export or test") }
	}

	wrappedContent?.let { ExportMenuBody(it) }
}

@Composable
private fun ExportMenuBody(wrappedContent: ContentWrapper) {
	// TODO BITSER Do something
}
