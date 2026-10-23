package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientStruct
import mardek.content.inventory.ItemType
import mardek.editor.client.component.*

@Composable
fun EditItemComponent(item: BitClientStruct, itemsContent: BitClientStruct, goBack: () -> Unit) {
	Column {
		Row {
			Text("General properties", style = LocalTextStyle.current.copy(fontSize = 1.5.em, fontWeight = FontWeight.Bold))
		}
		Row(modifier = Modifier.padding(vertical = 8.dp)) {
			Text("Display name:")
			BitserTextField(item.getSimpleField(null, "displayName"))
		}
		// TODO BITSER Sprite
		Row { Text("Description:") }
		Row { BitserBigTextField(item.getSimpleField(null, "description")) }
		Row {
			Text("Type:")
			BitserReferenceDropdown<ItemType>(
				item.getStructReferenceField(null, "type"),
				itemsContent, null, "itemTypes", 0
			) { it.displayName }
		}
		// TODO BITSER Element
		Row {
			Text("Cost (price):")
			BitserIntField(item.getSimpleField(null, "cost"))
		}
		// TODO BITSER Equipment
		// TODO BITSER Consumable
		Row { DedicatedStructsSaveButtons(arrayOf(item), goBack) }
	}

	TrackStruct(item)
}
