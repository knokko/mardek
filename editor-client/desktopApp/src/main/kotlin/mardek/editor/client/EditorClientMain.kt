package mardek.editor.client

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    val content = launchEditorClientConnection()
    Window(
        onCloseRequest = ::exitApplication,
        title = "MARDEK Editor",
        state = WindowState(size = DpSize(1500.dp, 800.dp))
    ) {
        App(content)
    }
}
