package mardek.editor.dummy

import androidx.compose.runtime.*
import com.github.knokko.bitser.connection.BitClient
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.editor.TEST_AUTH_TOKEN
import mardek.editor.client.inventory.ItemTypeComponent
import mardek.editor.view.generateDummyView1
import tech.kwik.core.QuicClientConnection
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.CompletableFuture

@Composable
fun DummyApp1(connection: QuicClientConnection, rootStruct: BitClient.Struct) {
	ItemTypeComponent(rootStruct)
}

fun launchDummyConnection1(): Pair<QuicClientConnection, CompletableFuture<BitClient.Struct>> {
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
		.build()

	var streamCounter = 0
	connection.setPeerInitiatedStreamCallback { stream ->
		stream.inputStream.read() // Skip the first (dummy) byte

		if (streamCounter == 0) {
			stream.outputStream.write(TEST_AUTH_TOKEN)
			stream.outputStream.flush()

			val rootConnection = BitClient.Struct(
				generateDummyView1(),
				BitOutputStream(stream.outputStream),
				{ stream.resetStream(0L) },
				0L
			)

			val mainReadThread = Thread {
				getRootStruct.complete(rootConnection)
				rootConnection.readFromServer(BitInputStream(stream.inputStream))
			}
			mainReadThread.isDaemon = true
			mainReadThread.start()
		} else if (streamCounter == 1) {
			while (true) {
				if (stream.inputStream.read() != 123) {
					connection.close()
					throw RuntimeException("Keep-alive stream sent unexpected byte")
				}
			}
		} else {
			throw RuntimeException("Not yet")
		}

		streamCounter += 1
	}
	connection.connect()

	return Pair(connection, getRootStruct)
}
