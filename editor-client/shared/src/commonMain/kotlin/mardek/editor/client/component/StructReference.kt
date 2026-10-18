package mardek.editor.client.component


import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun StructReference(
	mutateScope: CoroutineScope, parentStruct: BitClient.Struct,
	declaringClass: Class<*>?, fieldName: String, callback: (BitClient.Struct) -> Unit
) {
	DisposableEffect(parentStruct) {
		val referenceField = parentStruct.getStructReferenceField(declaringClass, fieldName)
		val childSubscription = referenceField.subscribe(false) {
			mutateScope.launch { callback(referenceField.createConnectionToReference(it)) }
		}
		onDispose {
			referenceField.cancelSubscription(childSubscription)
		}
	}
}
