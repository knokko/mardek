package mardek.editor.client.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch
import mardek.editor.client.fontSize
import java.lang.Integer.parseInt

@Composable
fun BitserIntField(field: BitClient.SimpleFlatField<Int>) {
	val mutateScope = rememberCoroutineScope()
	val textFieldState = rememberTextFieldState()
	var changeState by remember { mutableStateOf(BitClient.ChangeState.UNINITIALIZED) }

	TextField(
		state = textFieldState,
		modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, top = 10.dp, end = 30.dp),
		textStyle = TextStyle(
			fontSize = fontSize,
			color = if (changeState == BitClient.ChangeState.MODIFIED) Color.Blue else Color.Unspecified
		),
		contentPadding = PaddingValues(5.dp),
		lineLimits = TextFieldLineLimits.SingleLine,
		inputTransformation = {
			try {
				val intValue = parseInt(this.toString())
				field.set(intValue)
			} catch (invalid: NumberFormatException) {
				println("Invalid $this")
			}
		},
		enabled = changeState != BitClient.ChangeState.UNINITIALIZED,
	)

	DisposableEffect(field) {
		val valueSubscription = field.subscribe(true) {
			mutateScope.launch {
				val stringValue = it.toString()
				if (stringValue != textFieldState.text) {
					textFieldState.edit { replace(0, length, stringValue) }
				}
			}
		}

		val changeStateSubscription = field.subscribeChangeState { mutateScope.launch { changeState = it } }
		onDispose {
			field.cancelSubscription(valueSubscription)
			field.cancelSubscription(changeStateSubscription)
		}
	}
}
