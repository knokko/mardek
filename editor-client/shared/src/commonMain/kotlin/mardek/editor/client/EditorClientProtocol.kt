package mardek.editor.client

import com.github.knokko.bitser.kwik.BikClientProtocol
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import tech.kwik.core.QuicClientConnection
import tech.kwik.core.QuicStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.CertificateFactory

class EditorClientProtocol(
	private val trustStore: KeyStore,
	private val authToken: ByteArray,
) : BikClientProtocol {

	override fun configureConnection(builder: QuicClientConnection.Builder) {
		builder.customTrustStore(trustStore)
	}

	override fun runHandshake(stream: QuicStream) {
		stream.outputStream.write(authToken)
		stream.outputStream.flush()
	}

	companion object {

		fun createTrustStore(publicKeyInput: InputStream): KeyStore {
			val cf = CertificateFactory.getInstance("X.509")
			val certificate = cf.generateCertificate(publicKeyInput)
			publicKeyInput.close()

			val trustStore = KeyStore.getInstance("PKCS12")
			trustStore.load(null)
			trustStore.setCertificateEntry(EDITOR_KEY_ALIAS, certificate)
			return trustStore
		}

		fun createDevelopmentTrustStore() = createTrustStore(Files.newInputStream(
			File("$SERVER_CERTIFICATE_FOLDER/public-certificate.pem").toPath()
		))
	}
}
