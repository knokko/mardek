package mardek.editor.client

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import com.github.knokko.bitser.kwik.BikClient
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_PORT
import mardek.editor.EditorView
import mardek.editor.TEST_AUTH_TOKEN
import mardek.editor.client.component.ChildStruct
import mardek.editor.client.component.TrackStruct
import mardek.editor.client.inventory.ItemsContentTabs
import mardek.editor.client.navbar.EditorClientNavbar
import mardek.editor.client.navbar.NavbarTab
import java.net.URI

@Composable
fun App(content: BitClient.Struct) {
    val mutateScope = rememberCoroutineScope()
    var itemsContent by remember { mutableStateOf<BitClient.Struct?>(null) }

    var currentTab by remember { mutableStateOf(NavbarTab.Settings) }
    Column {
        EditorClientNavbar(currentTab) { currentTab = it }
        itemsContent?.let { ItemsContentTabs(it, currentTab) }
    }

    TrackStruct(content)
    ChildStruct(mutateScope, content, null, "items") { itemsContent = it }
}

fun launchEditorClientConnection() = BikClient.connect(
    URI("https://localhost:$EDITOR_PORT"),
    EDITOR_APPLICATION_PROTOCOL_NAME,
    EditorView.root,
    EditorClientProtocol(
        EditorClientProtocol.createDevelopmentTrustStore(), TEST_AUTH_TOKEN
    )
)!!
