package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.connection.BitClientStructList
import mardek.editor.EditorView
import mardek.editor.client.component.info.BitserInt
import mardek.editor.client.component.info.BitserText
import mardek.editor.client.component.tracking.StructReference
import mardek.editor.client.component.tracking.TrackStruct
import mardek.editor.client.template.StructListOverview
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.edit_24px
import org.jetbrains.compose.resources.painterResource

class ItemOverview(
	list: BitClientStructList,
	private val itemsContent: BitClientStruct,
) : StructListOverview(list) {

	private lateinit var itemControllerToEdit: MutableState<Long?>

	init {
		columns.add(Column(
			title = "Display name",
			modifier = Modifier.width(250.dp).padding(start = 10.dp),
		) {
			BitserText(it.getSimpleField(null, "displayName"))
		})
		columns.add(Column(
			title = "Item type",
			modifier = Modifier.width(250.dp),
		) {
			val mutateScope = rememberCoroutineScope()
			var itemType by remember { mutableStateOf<BitClientStruct?>(null) }

			Row {
				if (itemType == null) Text("Loading...")
				itemType?.let { itemType -> ItemTypeNameComponent(itemType) }
			}

			StructReference(mutateScope, it, null, "type", 2) { newType -> itemType = newType }
		})
		columns.add(Column(
			title = "Cost",
			modifier = Modifier.width(100.dp),
		) {
			BitserInt(it.getSimpleField(null, "cost"))
		})

		rightButtons.add(RightButton(
			modifier = Modifier.width(100.dp)
		) {
			Button(onClick = {
				itemControllerToEdit.value = it.controllerID
			}) {
				Icon(painterResource(Res.drawable.edit_24px), contentDescription = null)
			}
		})

		bottomButtons.add {
			Button(onClick = { list.executeOperation(EditorView.operationAddWeapon) {} }) {
				Text("Add weapon")
			}
		}
	}

	@Composable
	override fun initializeState() {
		super.initializeState()
		itemControllerToEdit = remember { mutableStateOf(null) }
	}

	override fun getViewId() = 1

	@Composable
	override fun RenderListOrChildScreen(controllerIDs: List<Long>) {
		val controllerToEdit = this.itemControllerToEdit.value
		if (controllerToEdit == null) {
			super.RenderListOrChildScreen(controllerIDs)
		} else {
			val item = list.createConnectionToElement(controllerToEdit, 1)
			EditItemComponent(item, itemsContent) { itemControllerToEdit.value = null }
		}
	}
}

@Composable
private fun ItemTypeNameComponent(itemType: BitClientStruct) {
	BitserText(itemType.getSimpleField(null, "displayName"))
	TrackStruct(itemType)
}
