package mardek.importer.inventory

import mardek.content.Content
import mardek.content.inventory.Dreamstone
import mardek.importer.util.parseActionScriptResource
import java.util.UUID

fun importItemsContent(content: Content, fatItemTypes: List<FatItemType>): List<FatItemType> {
	val itemData = parseActionScriptResource("mardek/importer/inventory/data.txt")

	val itemSheets = ItemSheets()
	importItems(content, itemData.variableAssignments["ItemList"]!!, fatItemTypes, itemSheets)

	for (index in 1 .. 16) content.items.dreamstones.add(Dreamstone(
		index, UUID.nameUUIDFromBytes("DreamStone$index".encodeToByteArray())
	))
	return fatItemTypes
}

class ItemParseException(message: String): RuntimeException(message)
