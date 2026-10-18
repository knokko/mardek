package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.client.component.BitserTextField
import mardek.editor.client.component.TrackStruct

@Composable
fun EditItemComponent(item: BitClient.Struct, goBack: () -> Unit) {
	Column {
		Text("Editing")
		BitserTextField(item.getSimpleField(null, "displayName"))
		Row { DedicatedStructsSaveButtons(arrayOf(item), goBack) }
	}

	TrackStruct(item)
}
