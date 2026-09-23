package mardek.editor.dummy

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.util.concurrent.TimeUnit

fun main() = application {
	val (connection, getRootStruct) = launchDummyConnection2()
	val rootStruct = getRootStruct.get(10, TimeUnit.SECONDS)
	Window(
		onCloseRequest = {
			exitApplication()
			connection.closeAndWait()
		},
		title = "DummyClient2",
		state = WindowState(size = DpSize(1500.dp, 800.dp))
	) {
		DummyApp2(connection, rootStruct)
	}
}
