package mardek.editor

import com.github.knokko.bitser.connection.BitFlatStructListConnectionView
import com.github.knokko.bitser.connection.BitFlatStructView
import com.github.knokko.bitser.connection.BitStructConnectionView
import com.github.knokko.bitser.connection.BitStructListConnectionView
import com.github.knokko.bitser.connection.BitStructListOperation
import com.github.knokko.bitser.connection.BitStructOperation
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.inventory.Item
import mardek.content.inventory.ItemType
import mardek.content.inventory.ItemsContent
import mardek.exporting.ContentWrapper

object EditorView {

	val root: BitStructConnectionView
	val mainItemTypesView: BitStructListConnectionView<ItemType>
	val mainItemsView: BitStructListConnectionView<Item>

	val operationSyncForExport: BitStructOperation
	val operationAddItemType: BitStructListOperation<ItemType>
	val operationAddWeapon: BitStructListOperation<Item>

	init {
		val nameOnlyItemTypeView = BitFlatStructView(BITSER.getProtocol(ItemType::class.java))
		nameOnlyItemTypeView.markSimpleField(null, "displayName")
		nameOnlyItemTypeView.finishRegistration()

		val flatItemTypesView = BitFlatStructListConnectionView<ItemType>(
			2, nameOnlyItemTypeView
		)

		val fullItemTypeView = BitStructConnectionView(
			1, BITSER.getProtocol(ItemType::class.java)
		)
		fullItemTypeView.markAllSimpleFields(true, true)
		fullItemTypeView.finishRegistration()

		this.mainItemTypesView = BitStructListConnectionView<ItemType>(
			1, fullItemTypeView.protocol
		)
		mainItemTypesView.addElementsView(fullItemTypeView)
		mainItemTypesView.addElementsView(nameOnlyItemTypeView.toRegularView(2))

		this.operationAddItemType = mainItemTypesView.addOperation()
		mainItemTypesView.finishRegistration()

		val nameOnlyItemTypeReferenceView = BitStructConnectionView(
			2, BITSER.getProtocol(ItemType::class.java)
		)
		nameOnlyItemTypeReferenceView.markSimpleField(null, "displayName", true, true) // TODO BITSER Set canWrite to false
		nameOnlyItemTypeReferenceView.finishRegistration()

		val nameOnlyItemTypesReferenceView = BitStructListConnectionView<ItemType>(
			2, nameOnlyItemTypeReferenceView.protocol
		)
		nameOnlyItemTypesReferenceView.addElementsView(nameOnlyItemTypeReferenceView)
		nameOnlyItemTypesReferenceView.finishRegistration()

		val itemView = BitStructConnectionView(1, BITSER.getProtocol(Item::class.java))
		itemView.markAllSimpleFields(true, true)
		itemView.markStructReferenceField(
			null, "type",
			listOf(nameOnlyItemTypeReferenceView), true, true
		)
		itemView.finishRegistration()

		this.mainItemsView = BitStructListConnectionView<Item>(1, itemView.protocol)
		mainItemsView.addElementsView(itemView)
		this.operationAddWeapon = mainItemsView.addOperation()
		mainItemsView.finishRegistration()

		val itemsContentView = BitStructConnectionView(
			1, BITSER.getProtocol(ItemsContent::class.java)
		)
		itemsContentView.markStructListField(null, "itemTypes", mainItemTypesView, false) // TODO BITSER Change to true, once we support it
		itemsContentView.markFlatStructListField(null, "itemTypes", flatItemTypesView, true)
		itemsContentView.markStructListField(null, "items", mainItemsView, true)
		itemsContentView.finishRegistration()

		val contentView = BitStructConnectionView(
			1, BITSER.getProtocol(Content::class.java)
		)
		contentView.markChildStructField(null, "items", listOf(itemsContentView), false) // TODO BITSER Change to true once we support it
		contentView.finishRegistration()

		val wrappedContentView = BitStructConnectionView(
			1, BITSER.getProtocol(ContentWrapper::class.java)
		)
		wrappedContentView.markChildStructField(null, "content", listOf(contentView), false) // TODO BITSER Change to true once we support it
		this.operationSyncForExport = wrappedContentView.addOperation()
		wrappedContentView.finishRegistration()

		this.root = wrappedContentView
	}
}
