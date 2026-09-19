package mardek.editor.server.dummy1

import com.github.knokko.bitser.connection.BitServer
import mardek.content.inventory.ItemType
import mardek.editor.*
import mardek.editor.server.EDITOR_KEY_PASSWORD
import mardek.editor.view.generateDummyView1
import tech.kwik.core.QuicConnection
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ApplicationProtocolConnection
import tech.kwik.core.server.ApplicationProtocolConnectionFactory
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.File
import java.security.KeyStore

private class ProtocolConnection(
	private val rootController: BitServer.StructController<*>,
	private val clientConnection: QuicConnection
) : ApplicationProtocolConnection {

	init {
		val thread = Thread {
			val mainStream = clientConnection.createStream(true)

			// It looks like the client cannot see the stream until the server writes the first byte
			mainStream.outputStream.write(0)
			mainStream.outputStream.flush()

			val authToken = mainStream.inputStream.readNBytes(AUTH_TOKEN_LENGTH)
			if (!authToken.contentEquals(TEST_AUTH_TOKEN)) {
				clientConnection.close()
				return@Thread
			}

			rootController.addClient(mainStream.outputStream, mainStream.inputStream) {
				mainStream.resetStream(0L)
			}
		}
		thread.isDaemon = true
		thread.start()
	}
}

private class ProtocolFactory(
	private val rootController: BitServer.StructController<*>
) : ApplicationProtocolConnectionFactory {

	override fun createConnection(
		protocol: String,
		quicConnection: QuicConnection
	): ApplicationProtocolConnection? {
		if (protocol != EDITOR_APPLICATION_PROTOCOL_NAME) {
			quicConnection.close()
			return null
		}
		return ProtocolConnection(rootController, quicConnection)
	}
}

fun main() {
	val config = ServerConnectionConfig.builder()
		.maxTotalPeerInitiatedBidirectionalStreams(0)
		.maxTotalPeerInitiatedUnidirectionalStreams(0)
		.maxOpenPeerInitiatedBidirectionalStreams(0)
		.maxOpenPeerInitiatedUnidirectionalStreams(0)
		.maxConnectionBufferSize(100_000L)
		.maxUnidirectionalStreamBufferSize(1000L)
		.maxBidirectionalStreamBufferSize(1000L)
		.build()

	val keyStore = KeyStore.getInstance(
		File("$SERVER_CERTIFICATE_FOLDER/key-store.p12"),
		EDITOR_KEY_PASSWORD.toCharArray()
	)

	val logger = SysOutLogger()

	val connector = ServerConnector.builder()
		.withPort(EDITOR_PORT)
		.withConfiguration(config)
		.withKeyStore(keyStore, EDITOR_KEY_ALIAS, EDITOR_KEY_PASSWORD.toCharArray())
		.withLogger(logger)
		.build()

	val rootStruct = ItemType("WEAPON: SWORD", 200, "Sword")
	val rootController = BitServer.StructController(
		0L, rootStruct, generateDummyView1()
	)

	connector.registerApplicationProtocol(
		EDITOR_APPLICATION_PROTOCOL_NAME,
		ProtocolFactory(rootController)
	)
	connector.start()
	println("Press ENTER to exit...")
	readln()
	println("Exiting...")
	connector.close()
	println("Stopped the server")
}
