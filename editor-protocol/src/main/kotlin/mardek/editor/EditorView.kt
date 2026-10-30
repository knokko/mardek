package mardek.editor

import com.github.knokko.bitser.connection.BitFlatStructListConnectionView
import com.github.knokko.bitser.connection.BitFlatStructView
import com.github.knokko.bitser.connection.BitStructConnectionView
import com.github.knokko.bitser.connection.BitStructListConnectionView
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.inventory.EquipmentProperties
import mardek.content.inventory.Item
import mardek.content.inventory.ItemType
import mardek.content.inventory.ItemsContent
import mardek.content.inventory.WeaponProperties
import mardek.content.sprite.KimSprite
import mardek.content.stats.Resistances
import java.util.UUID

object EditorView {

	val root: BitStructConnectionView

	val operationAddItemType: Int

	val operationAddWeapon: Int

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

		val nestedItemTypesView = BitStructListConnectionView<ItemType>(
			1, fullItemTypeView.protocol
		)
		nestedItemTypesView.addElementsView(fullItemTypeView)
		nestedItemTypesView.addElementsView(nameOnlyItemTypeView.toRegularView(2))

		this.operationAddItemType = nestedItemTypesView.addSimpleOperation { itemTypeList ->
			itemTypeList.add(ItemType("ITEM TYPE: NEW", -1, "New item type"))
			true
		}
		nestedItemTypesView.finishRegistration()

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

		val itemsView = BitStructListConnectionView<Item>(1, itemView.protocol)
		itemsView.addElementsView(itemView)
		this.operationAddWeapon = itemsView.addSimpleGlobalOperation<Content> { context ->
			val itemTypes = context.rootStruct.items.itemTypes
			if (itemTypes.isEmpty()) {
				itemTypes.add(ItemType(
					"ITEM: FIRST",
					rgb(200, 0, 0),
					"First item type",
				))
				context.controllerMapping.getListController(itemTypes).refreshClientConnections()
			}

			context.thisList.add(Item(
				id = UUID.randomUUID(),
				displayName = "New weapon",
				sprite = KimSprite(),
				description = "",
				type = itemTypes[0],
				element = null,
				cost = 123,
				equipment = EquipmentProperties(
					skills = ArrayList(0),
					stats = ArrayList(0),
					elementalBonuses = ArrayList(0),
					resistances = Resistances(),
					autoEffects = ArrayList(0),
					weapon = WeaponProperties(
						hitChance = 100,
						critChance = 10,
						hpDrain = 0f,
						mpDrain = 0f,
						effectiveAgainstCreatureTypes = ArrayList(0),
						effectiveAgainstElements = ArrayList(0),
						addEffects = ArrayList(0),
						hitSound = null,
					),
					gem = null,
					onlyUser = null,
					charismaticPerformanceChance = 0,
				),
				consumable = null,
			))
			context.controllerMapping.getListController(context.thisList).refreshClientConnections()
		}
		itemsView.finishRegistration()

		val itemsContentView = BitStructConnectionView(
			1, BITSER.getProtocol(ItemsContent::class.java)
		)
		itemsContentView.markStructListField(null, "itemTypes", nestedItemTypesView, false) // TODO BITSER Change to true, once we support it
		itemsContentView.markFlatStructListField(null, "itemTypes", flatItemTypesView, true)
		itemsContentView.markStructListField(null, "items", itemsView, true)
		itemsContentView.finishRegistration()

		val contentView = BitStructConnectionView(
			1, BITSER.getProtocol(Content::class.java)
		)
		contentView.markChildStructField(null, "items", listOf(itemsContentView), false) // TODO BITSER Change to true once we support it
		contentView.finishRegistration()

		this.root = contentView
	}
}
