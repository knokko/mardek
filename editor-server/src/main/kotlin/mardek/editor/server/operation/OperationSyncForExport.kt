package mardek.editor.server.operation

import com.github.knokko.bitser.IntegerBitser
import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.connection.BitServerStructController
import com.github.knokko.bitser.connection.BitServerStructOperation
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import com.github.knokko.vk2d.resource.CompressingOutputStream
import mardek.content.BITSER
import mardek.exporting.ContentWrapper
import java.lang.Math.toIntExact
import java.nio.file.Files
import kotlin.io.path.fileSize
import kotlin.system.measureNanoTime

class OperationSyncForExport : BitServerStructOperation<ContentWrapper> {

	private var responseID = 0

	override fun readRequestData(fromClient: BitInputStream) {
		responseID = toIntExact(IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
			0L, Long.MAX_VALUE, fromClient
		))
	}

	override fun execute(
		controller: BitServerStructController<ContentWrapper>,
		context: BitServerContext,
		connectionHandle: Any,
	) {
		val tempFile = Files.createTempFile("editor-server-content", ".bits")
		tempFile.toFile().deleteOnExit()

		context.globalQueue.add {
			val compressionTime = measureNanoTime {
				val output = CompressingOutputStream(tempFile, 5)
				val bitOutput = BitOutputStream(output)
				BITSER.serialize(context.rootStruct, bitOutput)
				bitOutput.finish()
			}
			println("Saving & compressing took ${compressionTime / 1_000_000} ms")

			println("respond to client...")
			context.respondToClient.send(connectionHandle, responseID) { toClient ->
				println("RESPOND TO CLIENT?")
				val transmissionTime = measureNanoTime {
					val fileSize = toIntExact(tempFile.fileSize())
					IntegerBitser.encodeFullInt(fileSize, toClient)
					val tempInput = Files.newInputStream(tempFile)
					val sendBuffer = ByteArray(1024 * 1024)
					var remaining = fileSize
					while (remaining > 0) {
						if (remaining < sendBuffer.size) {
							tempInput.readNBytes(sendBuffer, 0, remaining)
							toClient.write(sendBuffer.sliceArray(0 until remaining))
							break
						} else {
							tempInput.readNBytes(sendBuffer, 0, sendBuffer.size)
							toClient.write(sendBuffer)
							remaining -= sendBuffer.size
						}
					}
					println("RESPONDED TO CLIENT ($fileSize bytes)")
				}
				println("Transmission took ${transmissionTime / 1_000_000} ms")
			}
		}
	}
}
