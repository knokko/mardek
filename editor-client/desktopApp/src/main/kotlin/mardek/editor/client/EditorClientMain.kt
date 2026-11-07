package mardek.editor.client

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import mardek.exporting.ContentWrapper
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

fun main() {
    val contentQueue = ArrayBlockingQueue<ContentWrapper>(1)

    val composeThread = Thread {
        application {
            val content = launchEditorClientConnection()
            Window(
                onCloseRequest = ::exitApplication,
                title = "MARDEK Editor",
                state = WindowState(size = DpSize(1500.dp, 800.dp))
            ) {
                App(content, contentQueue)
            }
        }
    }
    composeThread.start()

    while (composeThread.isAlive) {
        val contentWrapper = contentQueue.poll(1, TimeUnit.SECONDS) ?: continue
        runEditorClientTest(contentWrapper)

        // Ignore any attempts to re-start before the current test run is finished
        contentQueue.poll(0, TimeUnit.SECONDS)
    }
}
