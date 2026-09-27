package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.client.component.BitserTextField
import mardek.editor.client.component.StructsSaveButton
import mardek.editor.client.component.TrackStruct

@Composable
fun ItemTypeComponent(itemType: BitClient.Struct) {
	Row {
		BitserTextField(itemType.getSimpleField(null, "displayName"))
		BitserTextField(itemType.getSimpleField(null, "niceName"))
		StructsSaveButton(arrayOf(itemType))
	}

	TrackStruct(itemType)
}
