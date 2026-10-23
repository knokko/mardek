package mardek.editor.client.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.knokko.bitser.connection.BitClientFlatStructList

@Composable
fun TrackFlatStructList(structList: BitClientFlatStructList<*>) {
	DisposableEffect(structList) {
		structList.start()
		onDispose { structList.close() }
	}
}
