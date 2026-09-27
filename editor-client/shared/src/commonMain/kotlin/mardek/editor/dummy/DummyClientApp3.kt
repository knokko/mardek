package mardek.editor.dummy

import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import com.github.knokko.bitser.kwik.BikClient
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_PORT
import mardek.editor.TEST_AUTH_TOKEN
import mardek.editor.client.EditorClientProtocol
import mardek.editor.client.component.ChildStructList
import mardek.editor.client.component.TrackStruct
import mardek.editor.client.inventory.ItemTypeListComponent
import mardek.editor.view.generateDummyView3
import java.net.URI

@Composable
fun DummyApp3(rootStruct: BitClient.Struct) {
	ItemsContentComponent(rootStruct)
}

@Composable
fun ItemsContentComponent(itemsContent: BitClient.Struct) {
	val mutateScope = rememberCoroutineScope()
	var itemTypes by remember { mutableStateOf<BitClient.StructList?>(null)}

	itemTypes?.let { ItemTypeListComponent(it) }

	TrackStruct(itemsContent)
	ChildStructList(mutateScope, itemsContent, null, "itemTypes") { itemTypes = it }
}

fun launchDummyConnection3() = BikClient.connect(
	URI("https://localhost:$EDITOR_PORT"),
	EDITOR_APPLICATION_PROTOCOL_NAME,
	generateDummyView3(),
	EditorClientProtocol(
		EditorClientProtocol.createDevelopmentTrustStore(), TEST_AUTH_TOKEN
	)
)!!
