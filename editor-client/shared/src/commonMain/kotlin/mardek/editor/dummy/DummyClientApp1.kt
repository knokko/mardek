package mardek.editor.dummy

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import jdk.internal.net.http.quic.QuicClient
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.editor.TEST_AUTH_TOKEN
import mardek.editor.view.generateDummyView1
import tech.kwik.core.QuicClientConnection
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.CompletableFuture

@Composable
fun DummyApp1(connection: QuicClientConnection) {
	Column {
		Text("DummyApp1")
	}
}

fun launchDummyConnection1(): Pair<QuicClientConnection, CompletableFuture<BitClient.ReadWriteStruct>> {
	val getRootStruct = CompletableFuture<BitClient.ReadWriteStruct>()

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
		.maxOpenPeerInitiatedUnidirectionalStreams(100)
		.maxOpenPeerInitiatedBidirectionalStreams(100)
		.build()

	val connectionThread = Thread {
		var isFirst = true
		connection.setPeerInitiatedStreamCallback { stream ->
			// TODO Figure out why this is never invoked
			println("Stream was opened: $isFirst")
			stream.inputStream.read() // Skip the first (dummy) byte

			if (isFirst) {
				isFirst = false
				stream.outputStream.write(TEST_AUTH_TOKEN)
				stream.outputStream.flush()

				val rootConnection = BitClient.ReadWriteStruct(
					generateDummyView1(),
					BitOutputStream(stream.outputStream),
					0L
				)

				val mainReadThread = Thread {
					println("Start reading main connection...")
					getRootStruct.complete(rootConnection)
					rootConnection.readFromServer(BitInputStream(stream.inputStream))
				}
				mainReadThread.isDaemon = true
				mainReadThread.start()
			} else {
				throw RuntimeException("Not yet")
			}
		}
		connection.connect()
		println("connected")
	}
	connectionThread.isDaemon = true
	connectionThread.start()

	return Pair(connection, getRootStruct)
}
