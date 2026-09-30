package mardek.editor.client.inventory

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.client.component.TrackStructList
import mardek.editor.client.component.TrackStructListElements
import mardek.editor.view.DummyView3

@Composable
fun ItemTypeListComponent(list: BitClient.StructList) {
	var controllerIDs by remember { mutableStateOf<LongArray?>(null) }

	controllerIDs?.let { ids ->
		LazyColumn {
			items(ids.toTypedArray(), key = { it }) { controllerID ->
				ItemTypeComponent(list.createElementConnectionFromControllerID(controllerID))
			}
		}
	}

	Button(onClick = { list.executeSimpleOperation(DummyView3.operationAddItemType) }) {
		Text("Add new")
	}

	TrackStructList(list)
	TrackStructListElements(list) { controllerIDs = it }
}
