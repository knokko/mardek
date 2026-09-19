package mardek.editor.server.dummy1

import com.github.knokko.bitser.connection.BitServer
import mardek.content.inventory.ItemType
import mardek.editor.*
import mardek.editor.server.EDITOR_KEY_PASSWORD
import mardek.editor.view.generateDummyView1
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
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
			println("creating stream...")
			val mainStream = clientConnection.createStream(true)
			println("created stream")

			mainStream.outputStream.write("hello world\n".toByteArray())

			val authToken = mainStream.inputStream.readNBytes(AUTH_TOKEN_LENGTH)
			mainStream.outputStream.write(100)
			mainStream.outputStream.flush()
			println("got auth token ${authToken.contentToString()}")
			if (!authToken.contentEquals(TEST_AUTH_TOKEN)) {
				clientConnection.close()
				return@Thread
			}

			rootController.addClient(mainStream.outputStream, mainStream.inputStream)
		}
		thread.isDaemon = true
		thread.start()
	}

	override fun acceptPeerInitiatedStream(stream: QuicStream) {
		super.acceptPeerInitiatedStream(stream)
		println("hey, a client connected")
		println("trying to read something ${stream.inputStream.read()}")
		stream.outputStream.write(78)
		stream.outputStream.flush()
		println("tried to write something")
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

	override fun maxConcurrentPeerInitiatedUnidirectionalStreams(): Int {
		return 100
	}

	override fun maxConcurrentPeerInitiatedBidirectionalStreams(): Int {
		return 100
	}
}

fun main() {
	val config = ServerConnectionConfig.builder()
		.maxTotalPeerInitiatedBidirectionalStreams(10)
		.maxTotalPeerInitiatedUnidirectionalStreams(10)
		.maxOpenPeerInitiatedBidirectionalStreams(10)
		.maxOpenPeerInitiatedUnidirectionalStreams(10)
//		.maxConnectionBufferSize(100_000L)
//		.maxUnidirectionalStreamBufferSize(1000L)
//		.maxBidirectionalStreamBufferSize(1000L)
		.build()

	val keyStore = KeyStore.getInstance(
		File("$SERVER_CERTIFICATE_FOLDER/key-store.p12"),
		EDITOR_KEY_PASSWORD.toCharArray()
	)

	val logger = SysOutLogger()
//	logger.logInfo(true)
//	logger.logPackets(true)
//	logger.logDebug(true)

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
