package mardek.editor.client.component

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch
import mardek.editor_client.shared.generated.resources.Res
import mardek.editor_client.shared.generated.resources.save_24px
import org.jetbrains.compose.resources.painterResource

@Composable
fun StructsSaveButton(structs: Array<BitClient.Struct?>) {
	val mutateScope = rememberCoroutineScope()
	val properStructs = structs.filterNotNull()
	val canSave = properStructs.map { remember { mutableStateOf(false) } }

	Button(onClick = {
		properStructs.forEach { it.save() }
	}, enabled = canSave.any { it.value }) {
		Icon(painterResource(Res.drawable.save_24px), contentDescription = null)
	}

	for ((index, bitStruct) in properStructs.withIndex()) {
		DisposableEffect(bitStruct) {
			val subscription = bitStruct.subscribeCanSave { mutateScope.launch { canSave[index].value = it } }
			onDispose {
				bitStruct.cancelSubscription(subscription)
			}
		}
	}
}
