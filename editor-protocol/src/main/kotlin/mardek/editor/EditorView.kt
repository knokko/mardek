package mardek.editor

import com.github.knokko.bitser.connection.StructConnectionView
import com.github.knokko.bitser.connection.StructListConnectionView
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

	val root: StructConnectionView

	val operationAddItemType: Int

	val operationAddWeapon: Int

	init {
		val itemTypeView = StructConnectionView(BITSER.getProtocol(ItemType::class.java))
		itemTypeView.markAllSimpleFields(true, true)
		itemTypeView.finishRegistration()

		val itemTypesView = StructListConnectionView<ItemType>(itemTypeView)
		this.operationAddItemType = itemTypesView.addSimpleOperation { itemTypeList ->
			itemTypeList.add(ItemType("ITEM TYPE: NEW", -1, "New item type"))
			true
		}
		itemTypesView.finishRegistration()

		val itemView = StructConnectionView(BITSER.getProtocol(Item::class.java))
		itemView.markAllSimpleFields(true, true)
		itemView.finishRegistration()

		val itemsView = StructListConnectionView<Item>(itemView)
		this.operationAddWeapon = itemsView.addSimpleOperation { itemList ->
			val firstItemType = ItemType() // TODO BITSER Choose a real item type
			itemList.add(Item(
				id = UUID.randomUUID(),
				displayName = "New weapon",
				sprite = KimSprite(),
				description = "",
				type = firstItemType,
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
			true
		}
		itemsView.finishRegistration()

		val itemsContentView = StructConnectionView(BITSER.getProtocol(ItemsContent::class.java))
		itemsContentView.markStructListField(null, "itemTypes", itemTypesView, false) // TODO BITSER Change to true, once we support it
		itemsContentView.markStructListField(null, "items", itemsView, true)
		itemsContentView.finishRegistration()

		val contentView = StructConnectionView(BITSER.getProtocol(Content::class.java))
		contentView.markChildStructField(null, "items", itemsContentView, false) // TODO BITSER Change to true once we support it
		contentView.finishRegistration()

		this.root = contentView
	}
}
