package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.github.knokko.bitser.connection.BitClientStruct
import mardek.editor.client.component.BitserIntField
import mardek.editor.client.component.TrackStruct

@Composable
fun WeaponPropertiesComponent(weapon: BitClientStruct) {
	Row {
		BitserIntField(weapon.getSimpleField(null, "hitChance"))
		BitserIntField(weapon.getSimpleField(null, "critChance"))
	}

	TrackStruct(weapon)
}
