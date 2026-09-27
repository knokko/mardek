package mardek.editor.view

import com.github.knokko.bitser.connection.StructConnectionView
import com.github.knokko.bitser.connection.StructListConnectionView
import mardek.content.BITSER
import mardek.content.inventory.ItemType
import mardek.content.inventory.ItemsContent

object DummyView3 {

	val root: StructConnectionView
	val operationAddItemType: Int

	init {
		val itemTypeView = StructConnectionView(BITSER.getProtocol(ItemType::class.java))
		itemTypeView.markAllSimpleFields(true, true)
		itemTypeView.finishRegistration()

		val itemTypesView = StructListConnectionView<ItemType>(itemTypeView)
		this.operationAddItemType = itemTypesView.addSimpleOperation { structList ->
			structList.add(ItemType("ITEM TYPE: NEW", -1, "New item type"))
			true
		}
		itemTypesView.finishRegistration()

		val contentView = StructConnectionView(BITSER.getProtocol(ItemsContent::class.java))
		contentView.markStructListField(null, "itemTypes", itemTypesView, false) // TODO BITSER Change to true, once we support it
		contentView.finishRegistration()

		this.root = contentView
	}
}
