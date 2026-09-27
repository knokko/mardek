package mardek.editor.server.dummy3

import com.github.knokko.bitser.kwik.BikServer
import com.github.knokko.bitser.kwik.BikServerProtocol
import mardek.content.inventory.ItemType
import mardek.content.inventory.ItemsContent
import mardek.editor.*
import mardek.editor.server.EDITOR_KEY_PASSWORD
import mardek.editor.view.generateDummyView3
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.File
import java.security.KeyStore

private class DummyEditorServerProtocol3 : BikServerProtocol {

	override fun configureConfig(builder: ServerConnectionConfig.Builder) {
		builder.maxConnectionBufferSize(10_000_000L)
	}

	override fun configureConnector(builder: ServerConnector.Builder) {
		val keyStore = KeyStore.getInstance(
			File("$SERVER_CERTIFICATE_FOLDER/key-store.p12"),
			EDITOR_KEY_PASSWORD.toCharArray()
		)

		val logger = SysOutLogger()

		builder.withPort(EDITOR_PORT)
		builder.withKeyStore(keyStore, EDITOR_KEY_ALIAS, EDITOR_KEY_PASSWORD.toCharArray())
		builder.withLogger(logger)
	}

	override fun runHandshake(clientConnection: QuicConnection, mainStream: QuicStream): Boolean {
		mainStream.outputStream.write(0)
		mainStream.outputStream.flush()

		val authToken = mainStream.inputStream.readNBytes(AUTH_TOKEN_LENGTH)
		return authToken.contentEquals(TEST_AUTH_TOKEN)
	}

	override fun waitUntilServerShouldStop() {
		println("Press ENTER to exit...")
		readln()
		println("Exiting...")
	}
}

fun main() {
	val rootStruct = ItemsContent()
	rootStruct.itemTypes.add(ItemType("WEAPON: SWORD", 123, "Sword"))
	rootStruct.itemTypes.add(ItemType("WEAPON: AXE", 124, "Axe"))

	BikServer.run(
		DummyEditorServerProtocol3(),
		EDITOR_APPLICATION_PROTOCOL_NAME,
		rootStruct,
		generateDummyView3(),
	)
}
