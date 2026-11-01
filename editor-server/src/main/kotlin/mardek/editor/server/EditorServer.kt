package mardek.editor.server

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.bitser.kwik.BikServer
import com.github.knokko.bitser.kwik.BikServerProtocol
import com.github.knokko.memory.MemorySnapshot
import mardek.content.BITSER
import mardek.content.Content
import mardek.editor.AUTH_TOKEN_LENGTH
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.EDITOR_PORT
import mardek.editor.EditorView
import mardek.editor.SERVER_CERTIFICATE_FOLDER
import mardek.editor.TEST_AUTH_TOKEN
import mardek.exporting.ContentWrapper
import mardek.exporting.DecompressingInputStream
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.BufferedOutputStream
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterOutputStream
import kotlin.jvm.java
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
		println("Run 'stop' to stop")
		val didStop = AtomicBoolean(false)

		val backupThread = Thread {
			while (!didStop.get()) {
				sleep(60_000)
				context.globalQueue.add(saveContent)
			}
		}
		backupThread.isDaemon = true
		backupThread.start()

		while (true) {
			val command = readln()
			if (command == "stop") break
			if (command == "memory") MemorySnapshot.take().debugDump()
			if (command == "save") saveContent()
			if (command == "gc") System.gc()
		}
		println("Exiting...")
		didStop.set(true)
	}
}

fun main() {
	val contentFile = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content.bits").toPath()
	val contentInput = BitInputStream(DecompressingInputStream(contentFile.toFile()))
	val contentFile2 = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content2.bits").toPath()
	val wrappedContent = BITSER.deserialize(
		ContentWrapper::class.java, contentInput, Bitser.BACKWARD_COMPATIBLE,
	)
	System.gc()

	val saveFileLock = Any()

	fun saveContent() {
		// TODO BITSER Put back-ups in a different file
		println("start saving content...")
//		val duration = measureNanoTime {
//			val output = BitOutputStream(BufferedOutputStream(
//				Files.newOutputStream(File("test1.bits").toPath())
//			))
//			BITSER.serialize(wrappedContent, output, Bitser.BACKWARD_COMPATIBLE)
//			output.finish()
//			System.gc()
//		}
//		println("saving took ${duration / 1_000_000} ms")
		val rawContentBytes: ByteArray
		val duration1 = measureNanoTime {
			rawContentBytes = BITSER.toBytes(wrappedContent, Bitser.BACKWARD_COMPATIBLE)
		}
		println("finished serializing raw content (took ${duration1 / 1000_000} ms)")
		Thread {
			val duration2 = measureNanoTime {
				synchronized(saveFileLock) {
					//Files.write(contentFile, ContentWrapper.compress(rawContentBytes, 10))
					Files.write(contentFile2, rawContentBytes)
				}
			}
			System.gc()
			println("finished saving content (took ${duration2 / 1000_000} ms)")
		}.start()
	}

	BikServer.run(
		EditorServerProtocol(::saveContent),
		EDITOR_APPLICATION_PROTOCOL_NAME,
		wrappedContent,
		listOf(EditorView.root),
	)

	saveContent()
}
