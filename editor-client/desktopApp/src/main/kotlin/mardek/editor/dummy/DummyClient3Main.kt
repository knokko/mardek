package mardek.editor.dummy

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
	val rootStruct = launchDummyConnection3()
	Window(
		onCloseRequest = ::exitApplication,
		title = "DummyClient3",
		state = WindowState(size = DpSize(1500.dp, 800.dp))
	) {
		DummyApp3(rootStruct)
	}
}
