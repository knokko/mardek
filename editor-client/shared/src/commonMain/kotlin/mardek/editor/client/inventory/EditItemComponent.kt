package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.github.knokko.bitser.connection.BitClientStruct
import kotlinx.coroutines.CoroutineScope
import mardek.content.inventory.ItemType
import mardek.editor.client.component.EditComponents
import mardek.editor.client.component.tracking.ChildStruct
import mardek.editor.client.component.tracking.DedicatedStructsSaveButtons
import mardek.editor.client.component.tracking.TrackStruct

@Composable
fun EditItemComponent(item: BitClientStruct, itemsContent: BitClientStruct, goBack: () -> Unit) {
	val mutateScope = rememberCoroutineScope()
	var equipment by remember { mutableStateOf<BitClientStruct?>(null) }
	var weapon by remember { mutableStateOf<BitClientStruct?>(null) }

	Column {
		EditComponents.HeaderRow("General properties")
		EditComponents.TextFieldRow(item, "displayName", "Display name", 200.dp)
		// TODO BITSER Sprite
		EditComponents.TextAreaRows(item, "description", "Description", 900.dp, 100.dp)
		EditComponents.IntFieldRow(item, "cost", "Cost (price)", 100.dp)
		EditComponents.ReferenceDropdownRow<ItemType>(
			item, "type", "Type", 200.dp,
			itemsContent, "itemTypes", 2
		) { it.displayName }
		// TODO BITSER Element
		equipment?.let { EquipmentPropertiesComponent(it, mutateScope) { newWeapon -> weapon = newWeapon } }
		weapon?.let { WeaponPropertiesComponent(it) }

		// TODO BITSER Consumable
		Row { DedicatedStructsSaveButtons(arrayOf(item, equipment, weapon), goBack) }
	}

	TrackStruct(item)
	ChildStruct(mutateScope, item, null, "equipment", 1) { equipment = it }
}

@Composable
fun EquipmentPropertiesComponent(equipment: BitClientStruct, mutateScope: CoroutineScope, setWeapon: (BitClientStruct?) -> Unit) {
	EditComponents.HeaderRow("Equipment properties")

	EditComponents.IntFieldRow(
		equipment,
		"charismaticPerformanceChance",
		"Charismatic performance chance",
		50.dp,
	)

	TrackStruct(equipment)
	ChildStruct(mutateScope, equipment, null, "weapon", 1, setWeapon)
}

@Composable
fun WeaponPropertiesComponent(weapon: BitClientStruct) {
	EditComponents.HeaderRow("Weapon properties")
	EditComponents.IntFieldRow(weapon, "hitChance", "Hit chance", 50.dp)
	EditComponents.IntFieldRow(weapon, "critChance", "Critical hit chance", 50.dp)

	TrackStruct(weapon)
}
