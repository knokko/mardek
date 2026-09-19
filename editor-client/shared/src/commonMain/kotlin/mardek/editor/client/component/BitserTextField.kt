package mardek.editor.client.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch
import mardek.editor.client.fontSize
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.save_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun BitserTextField(field: BitClient.SimpleFlatField<String>) {
	val mutateScope = rememberCoroutineScope()
	val textFieldState = rememberTextFieldState()
	var changeState by remember { mutableStateOf(BitClient.ChangeState.UNINITIALIZED) }

	TextField(
		state = textFieldState,
		modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, top = 10.dp, end = 30.dp),
		textStyle = TextStyle(fontSize = fontSize),
		contentPadding = PaddingValues(5.dp),
		lineLimits = TextFieldLineLimits.SingleLine,
		inputTransformation = { field.set(this.toString()) }
	)
	Text(changeState.toString())

	Button(onClick = { field.save() }, enabled = changeState == BitClient.ChangeState.MODIFIED) {
		Icon(painterResource(Res.drawable.save_24px), contentDescription = null)
	}

	DisposableEffect(field) {
		field.subscribe(true) {
			mutateScope.launch {
				if (it != textFieldState.text) {
					textFieldState.edit { replace(0, length, it) }
				}
			}
		}
		field.subscribeChangeState { mutateScope.launch { changeState = it } }
		onDispose {  } // TODO Cancel subscription
	}
}
