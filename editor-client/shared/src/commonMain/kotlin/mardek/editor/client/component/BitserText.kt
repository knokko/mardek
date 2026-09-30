package mardek.editor.client.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.knokko.bitser.connection.BitClient
import kotlinx.coroutines.launch

@Composable
fun BitserText(field: BitClient.SimpleFlatField<String>) {
	val mutateScope = rememberCoroutineScope()
	var value by remember { mutableStateOf("") }

	Text(value)

	DisposableEffect(field) {
		val valueSubscription = field.subscribe(true) {
			mutateScope.launch { value = it }
		}

		onDispose {
			field.cancelSubscription(valueSubscription)
		}
	}
}
