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
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.EditorView
import mardek.editor.client.component.TrackStructList
import mardek.editor.client.component.TrackStructListElements

@Composable
private fun ItemRow(list: BitClient.StructList, controllerID: Long, scrollState: ScrollState) {
	println("ItemRow($controllerID)")
	Row(modifier = Modifier.horizontalScroll(scrollState).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
		ItemComponent(list.createConnectionToElement(controllerID))
	}
}

@Composable
private fun InnerItemList(list: BitClient.StructList, controllerIDs: List<Long>) {
	println("InnerItemList")
	val scrollState = rememberScrollState()

	Column(modifier = Modifier.fillMaxSize()) {
		println("Column")
		Row(modifier = Modifier.background(Color.Yellow).horizontalScroll(scrollState)) {
			Text("Display name", modifier = baseModifiers[0], fontSize = fontSize)
			Text("Item type", modifier = baseModifiers[2], fontSize = fontSize)
		}
		Box(Modifier.weight(1f)) {
			println("Box")
			LazyColumn(modifier = Modifier.fillMaxHeight().background(Color.Cyan)) {
				println("LazyColumn ${System.identityHashCode(controllerIDs)}")
				items(controllerIDs, key = { it }) { controllerID ->
					ItemRow(list, controllerID, scrollState)
				}
			}
		}

		Row(modifier = Modifier.background(Color.Yellow).horizontalScroll(scrollState)) {
			Button(onClick = { list.executeSimpleOperation(EditorView.operationAddWeapon) }) {
				Text("Add weapon")
			}
		}
	}
}

@Composable
fun ItemOverview(list: BitClient.StructList) {
	println("ItemOverview")
	val controllerIDs = remember { mutableStateListOf<Long>() }
	var isLoading by remember { mutableStateOf(true) }

	if (isLoading) {
		Text("Loading...")
	} else {
		InnerItemList(list, controllerIDs)
	}

	TrackStructList(list)
	TrackStructListElements(list) {
		controllerIDs.clear()
		controllerIDs.addAll(it.toList())
		isLoading = false
	}
}
