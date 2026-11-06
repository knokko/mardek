package mardek.editor.server.operation

import com.github.knokko.bitser.IntegerBitser
import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.connection.BitServerStructController
import com.github.knokko.bitser.connection.BitServerStructOperation
import com.github.knokko.bitser.io.BitInputStream
import mardek.exporting.ContentWrapper
import java.lang.Math.toIntExact

class OperationSyncForExport : BitServerStructOperation<ContentWrapper> {

	private var responseID = 0

	override fun readRequestData(fromClient: BitInputStream) {
		responseID = toIntExact(IntegerBitser.decodeVariableIntegerUsingTerminatorBits(
			0L, Long.MAX_VALUE, fromClient
		))
		println("Got response ID $responseID")
	}

	override fun execute(
		controller: BitServerStructController<ContentWrapper>,
		context: BitServerContext,
		connectionHandle: Any,
	) {
		println("Execute export")
		context.respondToClient.send(connectionHandle, responseID) {
			IntegerBitser.encodeFullInt(123, it)
			println("Responded 123")
		}
	}
}
