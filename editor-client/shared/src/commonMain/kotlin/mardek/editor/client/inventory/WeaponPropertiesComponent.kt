package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch
import mardek.editor.client.component.BitserIntField
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.save_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun WeaponPropertiesComponent(weapon: BitClient.Struct) {
	Row {
		BitserIntField(weapon.getSimpleField(null, "hitChance"))
		BitserIntField(weapon.getSimpleField(null, "critChance"))
	}

	DisposableEffect(weapon) {
		weapon.start()
		onDispose {
			weapon.close()
		}
	}
}
