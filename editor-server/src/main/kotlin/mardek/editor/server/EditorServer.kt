package mardek.editor.server

import com.github.knokko.bitser.connection.BitServerOperationRegistry
import com.github.knokko.bitser.kwik.BikServer
import mardek.editor.EDITOR_APPLICATION_PROTOCOL_NAME
import mardek.editor.EditorView
import mardek.editor.server.operation.OperationCreateItemType
import mardek.editor.server.operation.OperationCreateWeapon
import mardek.editor.server.operation.OperationSyncForExport

fun main() {
	val wrappedContent = loadContent()

	val operationRegistry = BitServerOperationRegistry()
	operationRegistry.implement(EditorView.operationSyncForExport, ::OperationSyncForExport)
	operationRegistry.implement(EditorView.operationAddItemType, ::OperationCreateItemType)
	operationRegistry.implement(EditorView.operationAddWeapon, ::OperationCreateWeapon)
	operationRegistry.finishRegistration()

	BikServer.run(
		EditorServerProtocol(),
		EDITOR_APPLICATION_PROTOCOL_NAME,
		wrappedContent,
		listOf(EditorView.root),
		operationRegistry
	)

	saveContent(wrappedContent, null)
}
