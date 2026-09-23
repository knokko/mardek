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
fun WeaponPropertiesComponent(weaponField: BitClient.ChildStructField) {
	val mutateScope = rememberCoroutineScope()
	var weapon by remember { mutableStateOf<BitClient.Struct?>(null) }

	DisposableEffect(weaponField) {
		val subscription = weaponField.subscribe { weapon = it }
		onDispose { weaponField.cancelSubscription(subscription) }
	}

	if (weapon == null) return

	var canSave by remember { mutableStateOf(false) }

	Row {
		BitserIntField(weapon!!.getSimpleField(null, "hitChance"))
		BitserIntField(weapon!!.getSimpleField(null, "critChance"))
		Button(onClick = { weapon!!.save() }, enabled = canSave) {
			Icon(painterResource(Res.drawable.save_24px), contentDescription = null)
		}
	}

	DisposableEffect(weapon) {
		weapon!!.start()
		val subscription = weapon!!.subscribeCanSave { mutateScope.launch { canSave = it } }
		onDispose {
			weapon!!.cancelSubscription(subscription)
			weapon!!.close()
		}
	}
}
