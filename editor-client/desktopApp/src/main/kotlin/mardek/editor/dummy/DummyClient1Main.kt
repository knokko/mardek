package mardek.editor.dummy

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.util.concurrent.TimeUnit

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
    val (connection, getRootStruct) = launchDummyConnection1()
    val rootStruct = getRootStruct.get(10, TimeUnit.MINUTES)
    rootStruct.subscribeValue<String>(null, "displayName", true) {
        println("Changed displayName to $it")
    }
    Thread.sleep(3000)
    println("set value...")
    rootStruct.setValue(null, "displayName", "hello")
    rootStruct.saveValue(null, "displayName")
    println("saved value")
    Thread.sleep(3000)
    //connection.closeAndWait()
}
