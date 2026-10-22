package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClientStruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChildStruct(
	mutateScope: CoroutineScope, parentStruct: BitClientStruct,
	declaringClass: Class<*>?, fieldName: String, viewIndex: Int, callback: (BitClientStruct) -> Unit
) {
	DisposableEffect(parentStruct) {
		val childSubscription = parentStruct.subscribeChildStruct(declaringClass, fieldName, viewIndex) {
			mutateScope.launch { callback(it) }
		}
		onDispose {
			parentStruct.cancelChildStructSubscription(declaringClass, fieldName, childSubscription)
		}
	}
}
