package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.client.component.BitserIntField
import mardek.editor.client.component.ChildStruct
import mardek.editor.client.component.ReadWriteStruct
import mardek.editor.client.component.StructsSaveButton

@Composable
fun EquipmentPropertiesComponent(equipment: BitClient.Struct) {
	val mutateScope = rememberCoroutineScope()
	var weapon by remember { mutableStateOf<BitClient.Struct?>(null)}

	Row {
		weapon?.let { WeaponPropertiesComponent(it) }
		BitserIntField(equipment.getSimpleField(null, "charismaticPerformanceChance"))
		StructsSaveButton(arrayOf(equipment, weapon))
	}

	ReadWriteStruct(equipment)
	ChildStruct(mutateScope, equipment, null, "weapon") { weapon = it }
}
