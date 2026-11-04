package mardek.editor.server.operation

import com.github.knokko.bitser.connection.BitServerContext
import com.github.knokko.bitser.connection.BitServerStructListController
import com.github.knokko.bitser.connection.BitServerStructListOperation
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.content.inventory.EquipmentProperties
import mardek.content.inventory.Item
import mardek.content.inventory.ItemType
import mardek.content.inventory.WeaponProperties
import mardek.content.sprite.KimSprite
import mardek.content.stats.Resistances
import mardek.exporting.ContentWrapper
import java.util.UUID

class OperationCreateWeapon : BitServerStructListOperation<Item> {

	override fun readRequestData(fromClient: BitInputStream) {}

	override fun execute(
		controller: BitServerStructListController<Item>,
		context: BitServerContext
	) {
		val wasQueued = context.globalQueue.offer {
			val contentWrapper = context.rootStruct as ContentWrapper
			val itemTypes = contentWrapper.content.items.itemTypes
			if (itemTypes.isEmpty()) {
				itemTypes.add(ItemType(
					"ITEM: FIRST",
					rgb(200, 0, 0),
					"First item type",
				))
				context.controllerMapping.getListController(itemTypes).refreshClientConnections()
			}

			controller.structList.add(Item(
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
			controller.refreshClientConnections()
		}

		if (!wasQueued) {
			println("Warning: failed to enqueue weapon creation")
		}
	}
}
