package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChildStruct(
	mutateScope: CoroutineScope, parentStruct: BitClient.Struct,
	declaringClass: Class<*>?, fieldName: String, callback: (BitClient.Struct) -> Unit
) {
	DisposableEffect(parentStruct) {
		val childSubscription = parentStruct.subscribeChildStruct(declaringClass, fieldName) {
			mutateScope.launch { callback(it) }
		}
		onDispose {
			parentStruct.cancelChildStructSubscription(declaringClass, fieldName, childSubscription)
		}
	}
}
