package mardek.editor.client.inventory

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientStructList
import mardek.editor.EditorView
import mardek.editor.client.component.BitserTextField
import mardek.editor.client.component.InlineStructsSaveButton
import mardek.editor.client.template.StructListOverview

val fontSize = 1.1.em

class ItemTypeOverview(list: BitClientStructList) : StructListOverview(list) {

	init {
		columns.add(Column(
			title = "Upper name",
			modifier = Modifier.width(300.dp).padding(horizontal = 10.dp),
		) {
			BitserTextField(it.getSimpleField(null, "displayName"))
		})
		columns.add(Column(
			title = "Nice name",
			modifier = Modifier.width(300.dp).padding(horizontal = 10.dp),
		) {
			BitserTextField(it.getSimpleField(null, "niceName"))
		})

		rightButtons.add(RightButton(
			modifier = Modifier.width(100.dp)
		) {
			InlineStructsSaveButton(arrayOf(it))
		})

		bottomButtons.add {
			Button(onClick = { list.executeOperation(EditorView.operationAddItemType) {} }) {
				Text("Add new")
			}
		}
	}

	override fun getViewId() = 1
}
