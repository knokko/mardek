package mardek.editor.client.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.github.knokko.bitser.connection.BitClientChangeState
import com.github.knokko.bitser.connection.BitClientFlatStructList
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.connection.BitClientStructReferenceField
import kotlinx.coroutines.launch

@Composable
fun <T> BitserReferenceDropdown(
	field: BitClientStructReferenceField,
	sourceStruct: BitClientStruct, sourceClass: Class<*>?, sourceFieldName: String, sourceViewID: Int,
	createText: (T) -> String,
) {
	val mutateScope = rememberCoroutineScope()
	var options by remember { mutableStateOf<BitClientFlatStructList<T>?>(null) }

	options?.let { InnerBitserReferenceDropdown(field, it, createText) }

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

	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { mutateScope.launch {
		expanded = it
		if (!expanded) filter = ""
	}}) {
		val selectedIndex = selectableControllerIDs.indexOfFirst { it == selectedControllerID }
		TextField(
			value = filter.ifEmpty {
				if (selectedIndex == -1) "" else createText(selectableOptions[selectedIndex])
			},
			onValueChange = { mutateScope.launch { filter = it } },
			readOnly = false,
			modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
			textStyle = TextStyle.Default.copy(
				color = if (changeState == BitClientChangeState.MODIFIED) Color.Blue else Color.Unspecified
			),
		)

		ExposedDropdownMenu(expanded = expanded, onDismissRequest = { mutateScope.launch {
			expanded = false
			filter = ""
		}}) {
			for (index in selectableOptions.indices) {
				val item = selectableOptions[index]
				val itemText = createText(item)
				if (!itemText.contains(filter, ignoreCase = true)) continue
				val controllerID = selectableControllerIDs[index]
				DropdownMenuItem(
					text = { Text(itemText) },
					onClick = { mutateScope.launch {
						field.set(controllerID)
						expanded = false
						filter = ""
					} },
				)
			}
		}
	}

	TrackFlatStructList(options)

	DisposableEffect(field) {
		val selectedOptionSubscription = field.subscribe(true) { selectedControllerID = it }
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
