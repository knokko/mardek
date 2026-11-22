package mardek.editor.client

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.kwik.BikClient
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EditorView
import mardek.editor.client.component.ChildStruct
import mardek.editor.client.component.TrackStruct
import mardek.editor.client.file.FileTabs
import mardek.editor.client.inventory.ItemsContentTabs
import mardek.editor.client.navbar.EditorClientNavbar
import mardek.editor.client.navbar.NavbarTab
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.URI
import java.util.UUID

@Composable
fun App(wrappedContent: BitClientStruct, testRunState: TestRunState) {
    val mutateScope = rememberCoroutineScope()
    var content by remember { mutableStateOf<BitClientStruct?>(null) }

    content?.let { InnerApp(wrappedContent, it, testRunState) }

    TrackStruct(wrappedContent)
    ChildStruct(mutateScope, wrappedContent, null, "content", 1) { content = it }
}

@Composable
private fun InnerApp(
    wrappedContent: BitClientStruct,
    content: BitClientStruct,
    testRunState: TestRunState,
) {
    val mutateScope = rememberCoroutineScope()
    var itemsContent by remember { mutableStateOf<BitClientStruct?>(null) }

    var currentTab by remember { mutableStateOf(NavbarTab.Settings) }
    Column(modifier = Modifier.fillMaxWidth()) {
        EditorClientNavbar(currentTab) { currentTab = it }
        itemsContent?.let { ItemsContentTabs(it, currentTab) }
        FileTabs(wrappedContent, currentTab, testRunState)
    }

    TrackStruct(content)
    ChildStruct(mutateScope, content, null, "items", 1) { itemsContent = it }
}

fun launchEditorClientConnection(
    isNew: Boolean, propertiesFile: File = EditorClientProperties.FILE
): BitClientStruct {
    val properties = EditorClientProperties.load(propertiesFile)
    return BikClient.connect(
        URI("https://${properties.serverIp}:${properties.serverPort}"),
        EDITOR_APPLICATION_PROTOCOL_NAME,
        EditorView.root,
        1,
        EditorClientProtocol(EditorClientProtocol.createDefaultTrustStore()) { output, input ->
            val dataOutput = DataOutputStream(output)
            dataOutput.writeLong(properties.password.mostSignificantBits)
            dataOutput.writeLong(properties.password.leastSignificantBits)
            dataOutput.flush()

            if (isNew) {
                val dataInput = DataInputStream(input)
                properties.password = UUID(dataInput.readLong(), dataInput.readLong())
                properties.save(propertiesFile)
            }
        }
    )!!
}
