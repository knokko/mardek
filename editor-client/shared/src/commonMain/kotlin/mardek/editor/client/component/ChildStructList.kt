package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.connection.BitClientStructList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChildStructList(
	mutateScope: CoroutineScope, parentStruct: BitClientStruct,
	declaringClass: Class<*>?, fieldName: String, viewID: Int, callback: (BitClientStructList) -> Unit
) {
	DisposableEffect(parentStruct) {
		val listSubscription = parentStruct.subscribeNestedStructList(declaringClass, fieldName, viewID) {
			mutateScope.launch { callback(it) }
		}
		onDispose {
			parentStruct.cancelStructListSubscription(declaringClass, fieldName, listSubscription)
		}
	}
}
