package mardek.editor.server.operation

import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.connection.BitServerStructListController
import com.github.knokko.bitser.connection.BitServerStructListOperation
import com.github.knokko.bitser.io.BitInputStream
import mardek.content.inventory.ItemType

class OperationCreateItemType : BitServerStructListOperation<ItemType> {

	override fun readRequestData(fromClient: BitInputStream) {}

	override fun execute(
		controller: BitServerStructListController<ItemType>,
		context: BitServerContext,
	) {
		synchronized(controller) {
			controller.structList.add(ItemType("ITEM TYPE: NEW", -1, "New item type"))
			controller.refreshClientConnections()
		}
	}
}
