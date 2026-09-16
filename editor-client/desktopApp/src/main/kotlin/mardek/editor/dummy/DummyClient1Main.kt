package mardek.editor.dummy

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

//fun main() = application {
//    val connection = launchDummyConnection1()
//    launchDummyServer1()
//    Window(
//        onCloseRequest = ::exitApplication,
//        title = "DummyClient1",
//        state = WindowState(size = DpSize(1500.dp, 800.dp))
//    ) {
//        DummyApp1(connection)
//    }
//}

fun main() {
    launchDummyConnection1()
    Thread.sleep(15000)
}
