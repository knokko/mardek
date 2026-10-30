package mardek.editor.server

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.kwik.BikServer
import com.github.knokko.bitser.kwik.BikServerProtocol
import mardek.content.BITSER
import mardek.content.Content
import mardek.editor.AUTH_TOKEN_LENGTH
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.EditorView
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.editor.TEST_AUTH_TOKEN
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime

private class EditorServerProtocol(private val saveContent: () -> Unit) : BikServerProtocol {

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

	override fun waitUntilServerShouldStop(context: BitServerContext) {
		println("Press ENTER to exit...")
		val didStop = AtomicBoolean(false)

		val backupThread = Thread {
			while (!didStop.get()) {
				sleep(60_000)
				context.globalQueue.add(saveContent)
			}
		}
		backupThread.isDaemon = true
		backupThread.start()

		readln()
		println("Exiting...")
		didStop.set(true)
	}
}

fun main() {
	val contentFile = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content.bits").toPath()
	val content = BITSER.fromBytes(
		Content::class.java,
		Files.readAllBytes(contentFile),
		Bitser.BACKWARD_COMPATIBLE,
	)

	fun saveContent() {
		// TODO BITSER Put back-ups in a different file
		val duration = measureNanoTime {
			val contentBytes = BITSER.toBytes(content, Bitser.BACKWARD_COMPATIBLE)
			Files.write(contentFile, contentBytes)
		}
		println("Saving content took ${duration / 1_000_000} ms")
	}

	BikServer.run(
		EditorServerProtocol(::saveContent),
		EDITOR_APPLICATION_PROTOCOL_NAME,
		content,
		listOf(EditorView.root),
	)

	saveContent()
}
