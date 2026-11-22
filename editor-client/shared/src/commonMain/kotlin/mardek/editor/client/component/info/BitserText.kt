package mardek.editor.client.component.info

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.github.knokko.bitser.connection.BitClientSimpleFlatField
import kotlinx.coroutines.launch

@Composable
fun BitserText(field: BitClientSimpleFlatField<String>) {
	val mutateScope = rememberCoroutineScope()
	var value by remember { mutableStateOf("") }

	Text(value, modifier = Modifier.padding(2.dp), fontSize = 1.05.em)

	DisposableEffect(field) {
		val valueSubscription = field.subscribe(true) {
			mutateScope.launch { value = it }
		}

		onDispose {
			field.cancelSubscription(valueSubscription)
		}
	}
}
