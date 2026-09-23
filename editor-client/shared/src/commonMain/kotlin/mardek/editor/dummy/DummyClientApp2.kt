package mardek.editor.dummy

import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import com.github.knokko.bitser.connection.ClientStream
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.editor.TEST_AUTH_TOKEN
import mardek.editor.client.inventory.EquipmentPropertiesComponent
import mardek.editor.view.generateDummyView2
import tech.kwik.core.QuicClientConnection
import tech.kwik.core.QuicStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.CompletableFuture

@Composable
fun DummyApp2(connection: QuicClientConnection, rootStruct: BitClient.Struct) {
	EquipmentPropertiesComponent(rootStruct)
}

class DummyStreamFactory2(private val mainOutput: DataOutputStream) : ClientStream.Factory {

	private val nextStreamMapping = mutableMapOf<Int, CompletableFuture<QuicStream>>()
	private var nextStreamID = 0

	override fun createStream(controllerID: Long) = DummyStream2 { requestActualStream(controllerID) }

	private fun requestActualStream(controllerID: Long): CompletableFuture<QuicStream> {
		val future = CompletableFuture<QuicStream>()
		synchronized(mainOutput) {
			nextStreamMapping[nextStreamID] = future
			mainOutput.writeInt(nextStreamID)
			mainOutput.writeLong(controllerID)
			mainOutput.flush()
			nextStreamID += 1
		}
		return future
	}

	fun addStream(newStream: QuicStream) {
		val dataInput = DataInputStream(newStream.inputStream)
		val streamID = dataInput.readInt()

		val future = nextStreamMapping.remove(streamID) ?: throw IllegalStateException("Unexpected stream ID $streamID")

		future.complete(newStream)
	}
}

class DummyStream2(private val requestStream: () -> CompletableFuture<QuicStream>) : ClientStream {

	private val sendLock = Any()
	private var stream: CompletableFuture<QuicStream>? = null

	override fun start(processInput: ClientStream.InputReader) {
		synchronized(this) {
			if (stream != null) throw IllegalStateException("Already started")
			stream = requestStream()
			stream!!.whenComplete { quicStream, _ ->
				if (quicStream == null) return@whenComplete

				val readThread = Thread {
					try {
						processInput.read(BitInputStream(quicStream.inputStream))
					} finally {
						quicStream.resetStream(0L)
					}
				}
				readThread.isDaemon = true
				readThread.start()
			}
		}
	}

	override fun send(sendFrame: ClientStream.OutputWriter) {
		if (stream == null) throw IllegalStateException("Not yet started")

		synchronized(sendLock) {
			val output = BitOutputStream(stream!!.get().outputStream)
			sendFrame.write(output)
			output.flush()
		}
	}

	override fun close() {
		if (stream != null) stream!!.get().resetStream(0L)
	}
}

fun launchDummyConnection2(): Pair<QuicClientConnection, CompletableFuture<BitClient.Struct>> {
	val getRootStruct = CompletableFuture<BitClient.Struct>()

	val certificateInput = Files.newInputStream(File("$SERVER_CERTIFICATE_FOLDER/public-certificate.pem").toPath())
	val cf = CertificateFactory.getInstance("X.509")
	val certificate = cf.generateCertificate(certificateInput)
	certificateInput.close()

	val trustStore = KeyStore.getInstance("PKCS12")
	trustStore.load(null)
	trustStore.setCertificateEntry(EDITOR_KEY_ALIAS, certificate)

	val connection = QuicClientConnection.newBuilder()
		.uri(URI("https://localhost:$EDITOR_PORT"))
		.applicationProtocol(EDITOR_APPLICATION_PROTOCOL_NAME)
		.customTrustStore(trustStore)
		.maxOpenPeerInitiatedUnidirectionalStreams(0)
		.build()

	var streamFactory: DummyStreamFactory2? = null

	connection.setPeerInitiatedStreamCallback { stream ->

		if (streamFactory == null) {
			stream.inputStream.read() // Skip the first (dummy) byte
			stream.outputStream.write(TEST_AUTH_TOKEN)
			stream.outputStream.flush()

			streamFactory = DummyStreamFactory2(DataOutputStream(stream.outputStream))

			val rootConnection = BitClient.Struct(
				generateDummyView2(),
				streamFactory.createStream(0),
				streamFactory
			)
			getRootStruct.complete(rootConnection)

			val keepAliveThread = Thread {
				while (true) {
					if (stream.inputStream.read() != 123) {
						connection.close()
						throw RuntimeException("Keep-alive stream sent unexpected byte")
					}
				}
			}
			keepAliveThread.isDaemon = true
			keepAliveThread.start()
		} else {
			streamFactory.addStream(stream)
		}
	}
	connection.connect()

	return Pair(connection, getRootStruct)
}