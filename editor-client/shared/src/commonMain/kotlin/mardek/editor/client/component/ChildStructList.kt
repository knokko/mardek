package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChildStructList(
	mutateScope: CoroutineScope, parentStruct: BitClient.Struct,
	declaringClass: Class<*>?, fieldName: String, callback: (BitClient.StructList) -> Unit
) {
	DisposableEffect(parentStruct) {
		val listSubscription = parentStruct.subscribeStructList(declaringClass, fieldName) {
			mutateScope.launch { callback(it) }
		}
		onDispose {
			parentStruct.cancelStructListSubscription(declaringClass, fieldName, listSubscription)
		}
	}
}
