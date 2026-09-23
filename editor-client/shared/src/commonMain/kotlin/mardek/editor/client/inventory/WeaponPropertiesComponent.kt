package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.github.knokko.bitser.connection.BitClient
import mardek.editor.client.component.BitserIntField
import mardek.editor.client.component.ReadWriteStruct

@Composable
fun WeaponPropertiesComponent(weapon: BitClient.Struct) {
	Row {
		BitserIntField(weapon.getSimpleField(null, "hitChance"))
		BitserIntField(weapon.getSimpleField(null, "critChance"))
	}

	ReadWriteStruct(weapon)
}
