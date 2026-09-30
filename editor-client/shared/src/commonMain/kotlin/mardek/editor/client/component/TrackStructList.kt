package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClient

@Composable
fun TrackStructList(structList: BitClient.StructList) {
	DisposableEffect(structList) {
		structList.start()
		onDispose { structList.close() }
	}
}

@Composable
fun TrackStructListElements(structList: BitClient.StructList, callback: (LongArray) -> Unit) {
	DisposableEffect(structList) {
		val subscription = structList.subscribe(callback)
		onDispose { structList.cancelSubscription(subscription) }
	}
}
