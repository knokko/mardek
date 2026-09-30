package mardek.editor.client.inventory

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.EditorView
import mardek.editor.client.component.TrackStructList
import mardek.editor.client.component.TrackStructListElements

val fontSize = 1.1.em

val baseModifiers = arrayOf(300.dp, 300.dp, 300.dp).map { Modifier.width(it) }

@Composable
private fun ItemTypeRow(list: BitClient.StructList, controllerID: Long, scrollState: ScrollState) {
	println("ItemTypeRow($controllerID)")
	Row(modifier = Modifier.horizontalScroll(scrollState).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
		ItemTypeComponent(list.createConnectionToElement(controllerID))
//						Box(modifier = baseModifiers[0]) {
//							TextField(
//								state = rememberTextFieldState(initialText = itemType.displayName),
//								modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, top = 10.dp, end = 30.dp),
//								textStyle = TextStyle(fontSize = fontSize),
//								contentPadding = PaddingValues(5.dp),
//								lineLimits = TextFieldLineLimits.SingleLine,
//								inputTransformation = {
//									val newText = this.toString()
//									println("new text is $newText")
//								}
//							)
//						}
//
//						Text(itemType.gridColor.toString(), modifier = baseModifiers[1], fontSize = fontSize)
//						Text(itemType.niceName, modifier = baseModifiers[2], fontSize = fontSize)
	}
}

@Composable
private fun InnerItemTypeList(list: BitClient.StructList, controllerIDs: List<Long>) {
	println("InnerItemTypeList")
	val scrollState = rememberScrollState()

	Column(modifier = Modifier.fillMaxSize()) {
		println("Column")
		Row(modifier = Modifier.background(Color.Yellow).horizontalScroll(scrollState)) {
			Text("Upper name", modifier = baseModifiers[0], fontSize = fontSize)
			Text("Grid color", modifier = baseModifiers[2], fontSize = fontSize)
			Text("Nice name", modifier = baseModifiers[1], fontSize = fontSize)
		}
		Box(Modifier.weight(1f)) {
			println("Box")
			LazyColumn(modifier = Modifier.fillMaxHeight().background(Color.Cyan)) {
				println("LazyColumn ${System.identityHashCode(controllerIDs)}")
				items(controllerIDs, key = { it }) { controllerID ->
					ItemTypeRow(list, controllerID, scrollState)
				}
			}
		}

		Row(modifier = Modifier.background(Color.Yellow).horizontalScroll(scrollState)) {
			Button(onClick = { list.executeSimpleOperation(EditorView.operationAddItemType) }) {
				Text("Add new")
			}
		}
	}
}

@Composable
fun ItemTypeOverview(list: BitClient.StructList) {
	println("ItemTypeOverview")
	val controllerIDs = remember { mutableStateListOf<Long>() }
	var isLoading by remember { mutableStateOf(true) }

	if (isLoading) {
		Text("Loading...")
	} else {
		InnerItemTypeList(list, controllerIDs)
	}

	TrackStructList(list)
	TrackStructListElements(list) {
		controllerIDs.clear()
		controllerIDs.addAll(it.toList())
		isLoading = false
	}
}
