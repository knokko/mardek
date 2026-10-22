package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClientStruct

@Composable
fun TrackStruct(bitStruct: BitClientStruct) {
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
