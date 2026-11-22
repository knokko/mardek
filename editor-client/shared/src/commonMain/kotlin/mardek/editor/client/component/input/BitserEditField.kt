package mardek.editor.client.component.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.InputTransformation
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientChangeState
import com.github.knokko.bitser.connection.BitClientSimpleFlatField
import kotlinx.coroutines.launch
import java.lang.Integer.parseInt

@Composable
private fun <T> BitserEditField(
	field: BitClientSimpleFlatField<T>,
	modifier: Modifier,
	contentPadding: PaddingValues = PaddingValues(horizontal = 5.dp),
	isMultiline: Boolean = false,
	inputTransformation: InputTransformation,
) {
	val mutateScope = rememberCoroutineScope()
	val textFieldState = rememberTextFieldState()
	var changeState by remember { mutableStateOf(BitClientChangeState.UNINITIALIZED) }

	OutlinedTextField(
		state = textFieldState,
		modifier = modifier,
		textStyle = TextStyle(
			fontSize = 1.05.em,
			color = if (changeState == BitClientChangeState.MODIFIED) Color.Blue else Color.Unspecified
		),
		contentPadding = contentPadding,
		lineLimits = if (isMultiline) TextFieldLineLimits.MultiLine() else TextFieldLineLimits.SingleLine,
		inputTransformation = inputTransformation,
		enabled = changeState != BitClientChangeState.UNINITIALIZED,
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

private fun baseModifier(width: Dp? = null): Modifier {
	var modifier = Modifier.padding(5.dp).height(35.dp)
	if (width != null) modifier = modifier.width(width)
	return modifier
}

@Composable
fun BitserTextField(field: BitClientSimpleFlatField<String>, width: Dp? = null) = BitserEditField(
	field = field,
	modifier = baseModifier(width),
	inputTransformation = { field.set(this.toString()) },
)

@Composable
fun BitserIntField(field: BitClientSimpleFlatField<Int>, width: Dp? = null) = BitserEditField(
	field = field,
	modifier = baseModifier(width),
	inputTransformation = {
		try {
			val intValue = parseInt(this.toString())
			field.set(intValue)
		} catch (_: NumberFormatException) {
			// TODO BITSER Clearly mark it in the UI
			println("Invalid $this")
		}
	},
)

@Composable
fun BitserBigTextField(field: BitClientSimpleFlatField<String>, width: Dp, height: Dp) = BitserEditField(
	field = field,
	modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, top = 10.dp, end = 30.dp).width(width).height(height),
	contentPadding = PaddingValues(5.dp),
	isMultiline = true,
	inputTransformation = { field.set(this.toString()) },
)
