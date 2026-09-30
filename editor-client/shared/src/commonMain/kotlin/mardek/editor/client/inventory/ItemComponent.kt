package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.client.component.BitserText
import mardek.editor.client.component.TrackStruct

@Composable
fun ItemComponent(item: BitClient.Struct) {
	Row {
		BitserText(item.getSimpleField(null, "displayName"))
		// TODO BITSER item type name
		// TODO BITSER edit button
	}

	TrackStruct(item)
}
