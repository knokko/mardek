package mardek.editor.server

import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.kwik.BikServerProtocol
import com.github.knokko.memory.MemorySnapshot
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.content.ContentWrapper
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.DataInputStream
import java.io.File
import java.lang.Thread.sleep
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class EditorServerProtocol(
	private val properties: EditorServerProperties,
	private val accounts: EditorServerAccounts,
) : BikServerProtocol {

	override fun configureConfig(builder: ServerConnectionConfig.Builder) {
		builder.maxConnectionBufferSize(10_000_000L)
	}

	override fun configureConnector(builder: ServerConnector.Builder) {
		val keyStore = KeyStore.getInstance(
			File("$SERVER_CERTIFICATE_FOLDER/key-store.p12"),
			properties.password.toCharArray()
		)

		val logger = SysOutLogger()

		builder.withPort(properties.port)
		builder.withKeyStore(keyStore, EDITOR_KEY_ALIAS, properties.password.toCharArray())
		builder.withLogger(logger)
	}

	override fun runHandshake(clientConnection: QuicConnection, mainStream: QuicStream): Any? {
		mainStream.outputStream.write(0)
		mainStream.outputStream.flush()

		val dataInput = DataInputStream(mainStream.inputStream)
		val password = UUID(dataInput.readLong(), dataInput.readLong())
		return accounts.getByPassword(password)
	}

	override fun waitUntilServerShouldStop(context: BitServerContext) {
		println("Run 'stop' to stop")
		val didStop = AtomicBoolean(false)

		val backupThread = Thread {
			while (!didStop.get()) {
				sleep(60_000)
				context.globalQueue.offer {
					saveContent(context.rootStruct as ContentWrapper)
				}
				accounts.save()
			}
		}
		backupThread.name = "EditorServerBackup"
		backupThread.isDaemon = true
		backupThread.start()

		while (true) {
			val command = readln()
			if (command == "stop") break
			if (command == "memory") MemorySnapshot.take().debugDump()
			if (command == "save") {
				if (!context.globalQueue.offer {
						saveContent(context.rootStruct as ContentWrapper)
				}) {
					println("Global queue is full!")
				}
				accounts.save()
			}
			if (command == "gc") System.gc()
			if (command.startsWith("accounts ")) {
				accounts.executeCommand(command.substringAfter("accounts "), properties)
			}
		}
		println("Exiting...")
		didStop.set(true)
	}
}
