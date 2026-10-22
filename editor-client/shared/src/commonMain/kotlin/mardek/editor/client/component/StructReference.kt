package mardek.editor.client.component


import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClientStruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun StructReference(
	mutateScope: CoroutineScope, parentStruct: BitClientStruct,
	declaringClass: Class<*>?, fieldName: String, viewIndex: Int, callback: (BitClientStruct) -> Unit
) {
	DisposableEffect(parentStruct) {
		val referenceField = parentStruct.getStructReferenceField(declaringClass, fieldName)
		val childSubscription = referenceField.subscribe(false) {
			mutateScope.launch { callback(referenceField.createConnectionToReference(it, viewIndex)) }
		}
		onDispose {
			referenceField.cancelSubscription(childSubscription)
		}
	}
}
