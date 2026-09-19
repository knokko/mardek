package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.client.component.BitserTextField

@Composable
fun ItemTypeComponent(itemType: BitClient.ReadWriteStruct) {
	Row {
		BitserTextField(itemType.getSimpleField(null, "displayName"))
		BitserTextField(itemType.getSimpleField(null, "niceName"))
	}

	DisposableEffect(itemType) {
		onDispose { itemType.close() }
	}
}
