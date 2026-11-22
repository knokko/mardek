package mardek.editor.client.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientChangeState
import com.github.knokko.bitser.connection.BitClientSimpleFlatField
import kotlinx.coroutines.launch

@Composable
fun BitserTextField(field: BitClientSimpleFlatField<String>) {
	val mutateScope = rememberCoroutineScope()
	val textFieldState = rememberTextFieldState()
	var changeState by remember { mutableStateOf(BitClientChangeState.UNINITIALIZED) }

	OutlinedTextField(
		state = textFieldState,
		modifier = Modifier.padding(5.dp).height(35.dp),
		textStyle = TextStyle(
			fontSize = 1.05.em,
			color = if (changeState == BitClientChangeState.MODIFIED) Color.Blue else Color.Unspecified
		),
		contentPadding = PaddingValues(horizontal = 5.dp),
		lineLimits = TextFieldLineLimits.SingleLine,
		inputTransformation = { field.set(this.toString()) },
		enabled = changeState != BitClientChangeState.UNINITIALIZED,
	)

	DisposableEffect(field) {
		val valueSubscription = field.subscribe(true) {
			mutateScope.launch {
				if (it != textFieldState.text) {
					textFieldState.edit { replace(0, length, it) }
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
