package mardek.editor.server.dummy1

import mardek.content.Content
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.editor.server.EDITOR_KEY_PASSWORD
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ApplicationProtocolConnection
import tech.kwik.core.server.ApplicationProtocolConnectionFactory
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.File
import java.security.KeyStore

private class ProtocolConnection : ApplicationProtocolConnection {

	override fun acceptPeerInitiatedStream(stream: QuicStream) {
		super.acceptPeerInitiatedStream(stream)
		println("new stream: $stream")
	}
}

private class ProtocolFactory : ApplicationProtocolConnectionFactory {
	override fun createConnection(
		protocol: String,
		quicConnection: QuicConnection
	): ApplicationProtocolConnection? {
		if (protocol != EDITOR_APPLICATION_PROTOCOL_NAME) {
			quicConnection.close()
			return null
		}
		println("Opened a client connection: $quicConnection")
		return ProtocolConnection()
	}

	override fun maxConcurrentPeerInitiatedUnidirectionalStreams() = Int.MAX_VALUE

	override fun maxConcurrentPeerInitiatedBidirectionalStreams() = Int.MAX_VALUE
}

fun main() {
	val config = ServerConnectionConfig.builder()
		.maxOpenPeerInitiatedBidirectionalStreams(10)
		.build()

	val keyStore = KeyStore.getInstance(
		File("$SERVER_CERTIFICATE_FOLDER/key-store.p12"),
		EDITOR_KEY_PASSWORD.toCharArray()
	)

	val connector = ServerConnector.builder()
		.withPort(EDITOR_PORT)
		.withConfiguration(config)
		.withKeyStore(keyStore, EDITOR_KEY_ALIAS, EDITOR_KEY_PASSWORD.toCharArray())
		.withLogger(SysOutLogger())
		.build()

	connector.registerApplicationProtocol(EDITOR_APPLICATION_PROTOCOL_NAME, ProtocolFactory())
	connector.start()
	println("Press ENTER to exit...")
	readln()
	println("Exiting...")
	connector.close()
	println("Stopped the server")
}
