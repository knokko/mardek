package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClientStruct
import mardek.editor.client.component.BitserIntField
import mardek.editor.client.component.ChildStruct
import mardek.editor.client.component.TrackStruct
import mardek.editor.client.component.InlineStructsSaveButton

@Composable
fun EquipmentPropertiesComponent(equipment: BitClientStruct) {
	val mutateScope = rememberCoroutineScope()
	var weapon by remember { mutableStateOf<BitClientStruct?>(null) }

	Row {
		weapon?.let { WeaponPropertiesComponent(it) }
		BitserIntField(equipment.getSimpleField(null, "charismaticPerformanceChance"))
		InlineStructsSaveButton(arrayOf(equipment, weapon))
	}

	TrackStruct(equipment)
	ChildStruct(mutateScope, equipment, null, "weapon", 1) { weapon = it }
}
