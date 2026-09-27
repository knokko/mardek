package mardek.editor.view

import com.github.knokko.bitser.connection.StructConnectionView
import com.github.knokko.bitser.connection.StructListConnectionView
import mardek.content.BITSER
import mardek.content.inventory.ItemType
import mardek.content.inventory.ItemsContent

fun generateDummyView3(): StructConnectionView {

	val itemTypeView = StructConnectionView(BITSER.getProtocol(ItemType::class.java))
	itemTypeView.markAllSimpleFields(true, true)
	itemTypeView.finishRegistration()

	val itemTypesView = StructListConnectionView(itemTypeView)
	itemTypesView.allowOperation(StructListConnectionView.Operation.Add)
	itemTypesView.finishRegistration()

	val contentView = StructConnectionView(BITSER.getProtocol(ItemsContent::class.java))
	contentView.markStructListField(null, "itemTypes", itemTypesView, false) // TODO BITSER Change to true, once we support it
	contentView.finishRegistration()

	return contentView
}
