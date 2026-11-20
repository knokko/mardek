package mardek.editor.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        val isFirstTime = !EditorClientProperties.FILE.exists()
        application {
            var hasClientProperties by remember { mutableStateOf(!isFirstTime) }
            if (hasClientProperties) {
                val content = launchEditorClientConnection(isFirstTime)
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "MARDEK Editor",
                    state = WindowState(size = DpSize(1500.dp, 800.dp))
                ) {
                    App(content, testRunState)
                }
            } else {
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "Paste MARDEK editor settings",
                    state = WindowState(size = DpSize(900.dp, 380.dp))
                ) { PasteSettingsApp { hasClientProperties = true } }
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
