package mardek.editor.client.inventory

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.awaitCancellation
import mardek.editor.client.fontSize

@Composable
fun ItemTypeComponent(itemType: BitClient.ReadWriteStruct) {
	println("Call ItemTypeComponent")
	val displayName = rememberTextFieldState()
	var displayNameState by remember { mutableStateOf(BitClient.ReadWriteStruct.ChangeState.UNINITIALIZED) }

	Row {
		println("Call Row")
		Text("test")
		TextField(
			state = displayName,
			modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, top = 10.dp, end = 30.dp),
			textStyle = TextStyle(fontSize = fontSize),
			contentPadding = PaddingValues(5.dp),
			lineLimits = TextFieldLineLimits.SingleLine,
			inputTransformation = {
				itemType.setValue(null, "displayName", this.toString())
			}
		)
		Text(displayNameState.toString())
	}

	LaunchedEffect(itemType) {
		println("LaunchedEffect")
		try {
			itemType.subscribeValue<String>(null, "displayName", true) {
				println("changed to $it")
				displayName.setTextAndPlaceCursorAtEnd(it)
			}
			itemType.subscribeChangeState(null, "displayName") { displayNameState = it }
			awaitCancellation()
		} finally {
			println("Closing ItemTypeComponent")
			itemType.close()
		}
	}
}
