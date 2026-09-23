package mardek.editor.dummy

import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import com.github.knokko.bitser.kwik.BikClient
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_PORT
import mardek.editor.TEST_AUTH_TOKEN
import mardek.editor.client.EditorClientProtocol
import mardek.editor.client.inventory.EquipmentPropertiesComponent
import mardek.editor.view.generateDummyView2
import java.net.URI

@Composable
fun DummyApp2(rootStruct: BitClient.Struct) {
	EquipmentPropertiesComponent(rootStruct)
}

fun launchDummyConnection2() = BikClient.connect(
	URI("https://localhost:$EDITOR_PORT"),
	EDITOR_APPLICATION_PROTOCOL_NAME,
	generateDummyView2(),
	EditorClientProtocol(
		EditorClientProtocol.createDevelopmentTrustStore(), TEST_AUTH_TOKEN
	)
)!!
