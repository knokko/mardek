package mardek.editor.client

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import mardek.editor.client.file.runEditorClientTest
import java.lang.Thread.sleep

fun main() {
    val testRunState = TestRunState()

    val composeThread = Thread {
        application {
            val content = launchEditorClientConnection()
            Window(
                onCloseRequest = ::exitApplication,
                title = "MARDEK Editor",
                state = WindowState(size = DpSize(1500.dp, 800.dp))
            ) {
                App(content, testRunState)
            }
        }
    }
    composeThread.name = "EditorClientCompose"
    composeThread.start()

    while (composeThread.isAlive) {
        sleep(500)
        if (testRunState.content == null) continue
        runEditorClientTest(testRunState)
        testRunState.reset()
    }
}
