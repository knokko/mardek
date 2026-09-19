package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlin.time.Duration.Companion.seconds

@Composable
fun ItemTypeComponent(itemType: BitClient.ReadWriteStruct) {
	println("Call ItemTypeComponent")
	var displayName by remember { mutableStateOf("") }
	Row {
		println("Call Row")
		Text("test")
		Text(displayName)
	}

	LaunchedEffect(itemType) {
		println("LaunchedEffect")
		try {
			itemType.subscribeValue<String>(null, "displayName", true) {
				println("changed to $it")
				displayName = it
			}
			awaitCancellation()
		} finally {
			itemType.close()
		}
	}
}
