package mardek.editor.client.inventory

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch
import mardek.editor.client.component.BitserTextField
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.save_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun ItemTypeComponent(itemType: BitClient.Struct) {
	val mutateScope = rememberCoroutineScope()
	var canSave by remember { mutableStateOf(false) }

	Row {
		BitserTextField(itemType.getSimpleField(null, "displayName"))
		BitserTextField(itemType.getSimpleField(null, "niceName"))
		Button(onClick = { itemType.save() }, enabled = canSave) {
			Icon(painterResource(Res.drawable.save_24px), contentDescription = null)
		}
	}

	DisposableEffect(itemType) {
		val subscription = itemType.subscribeCanSave { mutateScope.launch { canSave = it } }
		onDispose {
			itemType.cancelSubscription(subscription)
			itemType.close()
		}
	}
}
