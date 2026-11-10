package mardek.editor.server.operation

import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.IntegerBitser
import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.connection.BitServerStructController
import com.github.knokko.bitser.connection.BitServerStructOperation
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.io.BitOutputStream
import mardek.content.BITSER
import mardek.editor.server.saveContent
import mardek.exporting.CompressingOutputStream
import mardek.exporting.ContentWrapper
import mardek.exporting.DecompressingInputStream
import java.lang.Math.toIntExact
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
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
				val output = CompressingOutputStream(tempFile.toFile(), 2)
				val bitOutput = BitOutputStream(output)
				BITSER.serialize(context.rootStruct, bitOutput)
				bitOutput.finish()
			}
			println("Compression took ${compressionTime / 1_000_000} ms")

			println("respond to client...")
			context.respondToClient.send(connectionHandle, responseID) { toClient ->
				// TODO BITSER Stream this properly
				println("RESPOND TO CLIENT?")
				val transmissionTime = measureNanoTime {
					val compressedBytes = Files.readAllBytes(tempFile)
					tempFile.deleteIfExists()
					IntegerBitser.encodeFullInt(compressedBytes.size, toClient)
					toClient.write(compressedBytes)
					println("RESPONDED TO CLIENT (${compressedBytes.size} bytes)")
				}
				println("Transmission took ${transmissionTime / 1_000_000} ms")
			}
		}
	}
}
