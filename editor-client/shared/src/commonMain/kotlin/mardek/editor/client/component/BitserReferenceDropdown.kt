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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch

@Composable
fun BitserReferenceDropdown(field: BitClient.StructReferenceField) {
	val mutateScope = rememberCoroutineScope()
	val selectableControllerIDs = remember { mutableStateListOf<Long>() }

	DisposableEffect(field) {


		onDispose {
		}
	}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun InnerBitserReferenceDropdown(field: BitClient.StructReferenceField, selectableControllerIDs: List<Long>) {
	val mutateScope = rememberCoroutineScope()
	var controllerID by remember { mutableStateOf<Long?>(null) }
	var changeState by remember { mutableStateOf(BitClient.ChangeState.UNINITIALIZED) }
	var expanded by remember { mutableStateOf(false) }

	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
		TextField(
			value = controllerID?.toString() ?: "nothing yet",
			onValueChange = {

			},
			readOnly = true,
			modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
		)
		ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			DropdownMenuItem(text = { Text("ID 2") }, onClick = { controllerID = 2 })
		}
	}

	for (controllerID in selectableControllerIDs) {

	}

	DisposableEffect(field) {
		val controllerIdSubscription = field.subscribe(true) { controllerID = it }
		val changeStateSubscription = field.subscribeChangeState { mutateScope.launch { changeState = it } }

		onDispose {
			field.cancelSubscription(controllerIdSubscription)
			field.cancelSubscription(changeStateSubscription)
		}
	}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OuterBitserReferenceDropdown(field: BitClient.StructReferenceField, selectableControllerIDs: List<Long>) {
	val mutateScope = rememberCoroutineScope()
	var controllerID by remember { mutableStateOf<Long?>(null) }
	var changeState by remember { mutableStateOf(BitClient.ChangeState.UNINITIALIZED) }
	var expanded by remember { mutableStateOf(false) }

	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
		TextField(
			value = controllerID?.toString() ?: "nothing yet",
			onValueChange = {

			},
			readOnly = true,
			modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
		)
		ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			DropdownMenuItem(text = { Text("ID 2") }, onClick = { controllerID = 2 })
		}
	}

	for (controllerID in selectableControllerIDs) {

	}

	DisposableEffect(field) {
		val controllerIdSubscription = field.subscribe(true) { controllerID = it }
		val changeStateSubscription = field.subscribeChangeState { mutateScope.launch { changeState = it } }

		onDispose {
			field.cancelSubscription(controllerIdSubscription)
			field.cancelSubscription(changeStateSubscription)
		}
	}
}