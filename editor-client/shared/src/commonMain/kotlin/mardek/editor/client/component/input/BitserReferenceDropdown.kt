package mardek.editor.client.component.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.github.knokko.bitser.connection.BitClientFlatStructList
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.connection.BitClientStructReferenceField
import kotlinx.coroutines.launch
import mardek.editor.client.component.tracking.TrackFlatStructList

@Composable
fun <T> BitserReferenceDropdown(
	field: BitClientStructReferenceField,
	sourceStruct: BitClientStruct, sourceClass: Class<*>?, sourceFieldName: String, sourceViewID: Int,
	width: Dp,
	createText: (T) -> String,
) {
	val mutateScope = rememberCoroutineScope()
	var options by remember { mutableStateOf<BitClientFlatStructList<T>?>(null) }

	options?.let { InnerBitserReferenceDropdown(field, it, width, createText) }

	DisposableEffect(field) {
		val optionsSubscription = sourceStruct.subscribeFlatStructList(
			sourceClass, sourceFieldName, sourceViewID
		) { mutateScope.launch { options = it } }

		onDispose {
			sourceStruct.cancelStructListSubscription(sourceClass, sourceFieldName, optionsSubscription)
		}
	}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T> InnerBitserReferenceDropdown(
	field: BitClientStructReferenceField,
	options: BitClientFlatStructList<T>,
	width: Dp,
	createText: (T) -> String,
) {
	val mutateScope = rememberCoroutineScope()
	var selectedControllerID by remember { mutableStateOf<Long?>(null) }
	var changeState by remember { mutableStateOf(BitClientChangeState.UNINITIALIZED) }
	var expanded by remember { mutableStateOf(false) }
	var filter by remember { mutableStateOf("") }

	var selectableControllerIDs by remember { mutableStateOf(LongArray(0)) }
	var selectableOptions by remember {
		@Suppress("UNCHECKED_CAST")
		mutableStateOf<Array<T>>(emptyArray<Any>() as Array<T>)
	}

	val selectedIndex = selectableControllerIDs.indexOfFirst { it == selectedControllerID }
	val textFieldState = rememberTextFieldState()

	LaunchedEffect(selectedControllerID) {
		if (selectedIndex != -1) {
			textFieldState.edit { replace(0, length, createText(selectableOptions[selectedIndex])) }
		}
	}

	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
		expanded = it
		if (!expanded) filter = ""
	}, modifier = Modifier.padding(5.dp)) {
		OutlinedTextField(
			state = textFieldState,
			readOnly = false,
			lineLimits = TextFieldLineLimits.SingleLine,
			contentPadding = PaddingValues(horizontal = 5.dp),
			modifier = Modifier.menuAnchor(
				ExposedDropdownMenuAnchorType.PrimaryEditable, true
			).width(width).height(35.dp),
			textStyle = TextStyle(
				fontSize = 1.05.em,
				color = if (changeState == BitClientChangeState.MODIFIED) Color.Blue else Color.Unspecified
			),
			inputTransformation = {
				filter = this.toString()
				expanded = true
			},
		)

		ExposedDropdownMenu(expanded = expanded, onDismissRequest = {
			expanded = false
			filter = ""
		}) {
			for (index in selectableOptions.indices) {
				val item = selectableOptions[index]
				val itemText = createText(item)
				if (!itemText.contains(filter, ignoreCase = true)) continue
				val controllerID = selectableControllerIDs[index]
				DropdownMenuItem(
					text = { Text(itemText) },
					onClick = {
						expanded = false
						filter = ""
						field.set(controllerID)
					},
				)
			}
		}
	}

	TrackFlatStructList(options)

	DisposableEffect(field) {
		val selectedOptionSubscription = field.subscribe(true) { mutateScope.launch {
			selectedControllerID = it
			expanded = false
		} }
		val changeStateSubscription = field.subscribeChangeState { mutateScope.launch { changeState = it } }
		val selectableOptionsSubscription = options.subscribe { newOptions, newControllerIDs -> mutateScope.launch {
			selectableOptions = newOptions
			selectableControllerIDs = newControllerIDs
		} }

		onDispose {
			field.cancelSubscription(selectedOptionSubscription)
			field.cancelSubscription(changeStateSubscription)
			options.cancelSubscription(selectableOptionsSubscription)
		}
	}
}
