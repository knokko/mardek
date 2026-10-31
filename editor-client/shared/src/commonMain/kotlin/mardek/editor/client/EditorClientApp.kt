package mardek.editor.client

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClientStruct
import com.github.knokko.bitser.kwik.BikClient
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_PORT
import mardek.editor.EditorView
import mardek.editor.TEST_AUTH_TOKEN
import mardek.editor.client.component.ChildStruct
import mardek.editor.client.component.TrackStruct
import mardek.editor.client.file.FileTabs
import mardek.editor.client.inventory.ItemsContentTabs
import mardek.editor.client.navbar.EditorClientNavbar
import mardek.editor.client.navbar.NavbarTab
import java.net.URI

@Composable
fun App(wrappedContent: BitClientStruct) {
    val mutateScope = rememberCoroutineScope()
    var content by remember { mutableStateOf<BitClientStruct?>(null) }

    content?.let { InnerApp(wrappedContent, it) }

    TrackStruct(wrappedContent)
    ChildStruct(mutateScope, wrappedContent, null, "content", 1) { content = it }
}

@Composable
private fun InnerApp(wrappedContent: BitClientStruct, content: BitClientStruct) {
    val mutateScope = rememberCoroutineScope()
    var itemsContent by remember { mutableStateOf<BitClientStruct?>(null) }

    var currentTab by remember { mutableStateOf(NavbarTab.Settings) }
    Column {
        EditorClientNavbar(currentTab) { currentTab = it }
        itemsContent?.let { ItemsContentTabs(it, currentTab) }
        FileTabs(wrappedContent, currentTab)
    }

    TrackStruct(content)
    ChildStruct(mutateScope, content, null, "items", 1) { itemsContent = it }
}

fun launchEditorClientConnection() = BikClient.connect(
    URI("https://localhost:$EDITOR_PORT"),
    EDITOR_APPLICATION_PROTOCOL_NAME,
    EditorView.root,
    1,
    EditorClientProtocol(
        EditorClientProtocol.createDevelopmentTrustStore(), TEST_AUTH_TOKEN
    )
)!!
