package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClient

@Composable
fun TrackStruct(bitStruct: BitClient.Struct) {
	DisposableEffect(bitStruct) {
		try {
			bitStruct.start()
		} catch (failed: Throwable) {
			println("Failed to start $bitStruct")
			throw failed
		}
		onDispose { bitStruct.close() }
	}
}
