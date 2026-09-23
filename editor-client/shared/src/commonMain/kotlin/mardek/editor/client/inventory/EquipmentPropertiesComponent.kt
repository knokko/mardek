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
import mardek.editor.client.component.BitserTextField
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.save_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun EquipmentPropertiesComponent(equipment: BitClient.Struct) {
	val mutateScope = rememberCoroutineScope()
	var canSave by remember { mutableStateOf(false) }

	Row {
		BitserIntField(equipment.getSimpleField(null, "charismaticPerformanceChance"))
//		BitserTextField(itemType.getSimpleField(null, "displayName"))
//		BitserTextField(itemType.getSimpleField(null, "niceName"))
		Button(onClick = { equipment.save() }, enabled = canSave) {
			Icon(painterResource(Res.drawable.save_24px), contentDescription = null)
		}
	}

	DisposableEffect(equipment) {
		equipment.start()
		val subscription = equipment.subscribeCanSave { mutateScope.launch { canSave = it } }
		onDispose {
			equipment.cancelSubscription(subscription)
			equipment.close()
		}
	}
}
