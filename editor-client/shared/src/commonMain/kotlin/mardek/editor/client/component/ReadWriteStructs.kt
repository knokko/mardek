package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClient

@Composable
fun ReadWriteStruct(bitStruct: BitClient.Struct) {
	DisposableEffect(bitStruct) {
		bitStruct.start()
		onDispose { bitStruct.close() }
	}
}
