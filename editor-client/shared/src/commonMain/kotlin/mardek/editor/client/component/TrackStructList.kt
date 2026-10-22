package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClientStructList

@Composable
fun TrackStructList(structList: BitClientStructList) {
	DisposableEffect(structList) {
		structList.start()
		onDispose { structList.close() }
	}
}

@Composable
fun TrackStructListElements(structList: BitClientStructList, callback: (LongArray) -> Unit) {
	DisposableEffect(structList) {
		val subscription = structList.subscribe(callback)
		onDispose { structList.cancelSubscription(subscription) }
	}
}
