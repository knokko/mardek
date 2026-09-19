package mardek.editor.client.inventory

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.knokko.bitser.connection.BitClient
import com.github.knokko.bitser.connection.BitClient.ReadWriteStruct.ChangeState
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import mardek.editor.client.fontSize
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.save_24px
import org.jetbrains.compose.resources.painterResource
import java.awt.SystemColor.text
import kotlin.coroutines.coroutineContext

@Composable
fun ItemTypeComponent(itemType: BitClient.ReadWriteStruct) {
	println("Call ItemTypeComponent")
	val mutateScope = rememberCoroutineScope()
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
				println("should change from $originalText to $this")
				itemType.setValue(null, "displayName", this.toString())
			}
		)
		Text(displayNameState.toString())

		Button(onClick = { itemType.saveValue(null, "displayName") }, enabled = displayNameState == ChangeState.MODIFIED) {
			Icon(painterResource(Res.drawable.save_24px), contentDescription = null)
		}
	}

	LaunchedEffect(itemType) {
		println("LaunchedEffect")
		try {
			itemType.subscribeValue<String>(null, "displayName", true) {
				mutateScope.launch {
					if (it != displayName.text) {
						displayName.edit { replace(0, length, it) }
					}
				}
			}
			itemType.subscribeChangeState(null, "displayName") { displayNameState = it }
			awaitCancellation()
		} finally {
			println("Closing ItemTypeComponent")
			itemType.close()
		}
	}
}
