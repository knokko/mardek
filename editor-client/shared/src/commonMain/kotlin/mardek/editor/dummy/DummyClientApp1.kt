package mardek.editor.dummy

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import tech.kwik.core.QuicClientConnection
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.CertificateFactory

@Composable
fun DummyApp1(connection: QuicClientConnection) {
	Column {
		Text("DummyApp1")
	}
}

fun launchDummyConnection1(): QuicClientConnection {
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

	val connectionThread = Thread {
		try {
			connection.connect()
			println("connected?")
		} finally {
			connection.close()
		}
	}
	connectionThread.isDaemon = true
	connectionThread.start()

	return connection
}
