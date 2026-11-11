package mardek.editor.server

import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.kwik.BikServerProtocol
import com.github.knokko.memory.MemorySnapshot
import mardek.editor.AUTH_TOKEN_LENGTH
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.editor.TEST_AUTH_TOKEN
import mardek.exporting.ContentWrapper
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.File
import java.lang.Thread.sleep
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean

class EditorServerProtocol : BikServerProtocol {

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
		println("Run 'stop' to stop")
		val didStop = AtomicBoolean(false)

		val backupThread = Thread {
			while (!didStop.get()) {
				sleep(60_000)
				context.globalQueue.add { saveContent(context.rootStruct as ContentWrapper, null)}
			}
		}
		backupThread.name = "EditorServerBackup"
		backupThread.isDaemon = true
		backupThread.start()

		while (true) {
			val command = readln()
			if (command == "stop") break
			if (command == "memory") MemorySnapshot.take().debugDump()
			if (command == "save") saveContent(context.rootStruct as ContentWrapper, null)
			if (command == "gc") System.gc()
		}
		println("Exiting...")
		didStop.set(true)
	}
}
