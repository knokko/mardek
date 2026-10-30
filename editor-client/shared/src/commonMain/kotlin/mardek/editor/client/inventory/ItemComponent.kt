package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClientStruct
import mardek.editor.client.component.*
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.edit_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun ItemComponent(item: BitClientStruct, editItem: (Long) -> Unit) {
	val mutateScope = rememberCoroutineScope()
	var itemType by remember { mutableStateOf<BitClientStruct?>(null) }

	Row {
		BitserText(item.getSimpleField(null, "displayName"))
		if (itemType == null) Text("Loading...")
		itemType?.let { ItemTypeNameComponent(it) }
		BitserInt(item.getSimpleField(null, "cost"))
		Button(onClick = {
			editItem(item.controllerID)
			println("EDIT!!")
		}) {
			Icon(painterResource(Res.drawable.edit_24px), contentDescription = null)
		}
	}

	TrackStruct(item)
	StructReference(mutateScope, item, null, "type", 2) { itemType = it }
}

@Composable
private fun ItemTypeNameComponent(itemType: BitClientStruct) {
	BitserText(itemType.getSimpleField(null, "displayName"))
	TrackStruct(itemType)
}
