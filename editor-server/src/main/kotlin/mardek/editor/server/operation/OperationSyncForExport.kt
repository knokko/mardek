package mardek.editor.server.operation

import com.github.knokko.bitser.IntegerBitser
import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.connection.BitServerStructController
import com.github.knokko.bitser.connection.BitServerStructOperation
import com.github.knokko.bitser.io.BitInputStream
import mardek.exporting.ContentWrapper

class OperationSyncForExport : BitServerStructOperation<ContentWrapper> {

	private var responseID = 0

	override fun readRequestData(fromClient: BitInputStream) {
		responseID = IntegerBitser.decodeFullInt(fromClient)
	}

	override fun execute(
		controller: BitServerStructController<ContentWrapper>,
		context: BitServerContext
	) {
		println("Execute export")
	}
}
