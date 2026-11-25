package mardek.editor.client.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientStruct
import mardek.editor.client.component.input.BitserBigTextField
import mardek.editor.client.component.input.BitserIntField
import mardek.editor.client.component.input.BitserReferenceDropdown
import mardek.editor.client.component.input.BitserTextField

object EditComponents {

	@Composable
	fun HeaderRow(header: String) {
		Row(modifier = Modifier.padding(top = 25.dp)) { Text(header, fontWeight = FontWeight.Bold, fontSize = 1.4.em) }
	}

	@Composable
	fun FieldRow(label: String, inputField: @Composable () -> Unit) {
		Row {
			Column(modifier = Modifier.align(Alignment.CenterVertically)) { Text("$label: ", fontSize = 1.05.em) }
			Column(modifier = Modifier.align(Alignment.CenterVertically)) { inputField() }
		}
	}

	@Composable
	fun TextFieldRow(structInstance: BitClientStruct, fieldName: String, label: String, width: Dp) = FieldRow(label) {
		BitserTextField(structInstance.getSimpleField(null, fieldName), width)
	}

	@Composable
	fun IntFieldRow(structInstance: BitClientStruct, fieldName: String, label: String, width: Dp) = FieldRow(label) {
		BitserIntField(structInstance.getSimpleField(null, fieldName), width)
	}

	@Composable
	fun TextAreaRows(structInstance: BitClientStruct, fieldName: String, label: String, width: Dp, height: Dp) {
		Row { Text("$label:", fontSize = 1.05.em) }
		Row { BitserBigTextField(structInstance.getSimpleField(null, fieldName), width, height) }
	}

	@Composable
	fun <T> ReferenceDropdownRow(
		structInstance: BitClientStruct, fieldName: String, label: String, width: Dp,
		sourceStruct: BitClientStruct, sourceFieldName: String, sourceViewID: Int,
		createText: (T) -> String,
	) = FieldRow(label) {
		BitserReferenceDropdown(
			structInstance.getStructReferenceField(null, fieldName),
			sourceStruct, null, sourceFieldName, sourceViewID, width, createText,
		)
	}
}
