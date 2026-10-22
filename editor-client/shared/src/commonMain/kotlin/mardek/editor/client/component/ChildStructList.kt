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
	declaringClass: Class<*>?, fieldName: String, viewIndex: Int, callback: (BitClientStructList) -> Unit
) {
	DisposableEffect(parentStruct) {
		val listSubscription = parentStruct.subscribeStructList(declaringClass, fieldName, viewIndex) {
			mutateScope.launch { callback(it) }
		}
		onDispose {
			parentStruct.cancelStructListSubscription(declaringClass, fieldName, listSubscription)
		}
	}
}
