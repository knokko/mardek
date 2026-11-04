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
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
	val contentFile = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content.bits")
	val compressedContentFile = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content-compressed.bits.tmp")
	val contentInput = BitInputStream(DecompressingInputStream(contentFile))
	val wrappedContent = BITSER.deserialize(
		ContentWrapper::class.java, contentInput, Bitser.BACKWARD_COMPATIBLE,
	)
	System.gc()

	fun saveContent() {
		// TODO BITSER Put back-ups in a different file
		println("start saving content...")
		val saveDuration = measureNanoTime {
			synchronized(compressedContentFile) {
				val output = BitOutputStream(Files.newOutputStream(compressedContentFile.toPath()))
				BITSER.serialize(wrappedContent, output, Bitser.BACKWARD_COMPATIBLE)
				output.finish()
			}
			System.gc()
		}
		println("saving took ${saveDuration / 1_000_000} ms")

		Thread {
			val compressDuration = measureNanoTime {
				val tempContentFile = Files.createTempFile("", ".bits").toFile()
				tempContentFile.deleteOnExit()

				val compressionProcessBuilder = ProcessBuilder(
					"zstd",
					compressedContentFile.absolutePath,
					"-o",
					tempContentFile.absolutePath,
					"-10",
					"--force",
					"--rm",
					"--no-progress",
					"--quiet",
				)
				compressionProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
				compressionProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)

				synchronized(compressedContentFile) {
					val compressionProcess = compressionProcessBuilder.start()
					compressionProcess.waitFor(1, TimeUnit.MINUTES)
					if (compressionProcess.exitValue() != 0) {
						throw RuntimeException("Compression failed")
					}
				}

				synchronized(contentFile) {
					Files.move(
						tempContentFile.toPath(),
						contentFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
					)
				}
				System.gc()
			}
			println("Compression after save took ${compressDuration / 1_000_000} ms")
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
