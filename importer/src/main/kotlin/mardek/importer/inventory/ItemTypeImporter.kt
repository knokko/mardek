package mardek.importer.inventory

import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.ItemType
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

internal fun sheet(type: String): BufferedImage {
	val input = ItemParseException::class.java.getResourceAsStream("itemsheet_$type.png")
		?: throw ItemParseException("Can't find sheet $type")
	val sheet = ImageIO.read(input)
	input.close()
	return sheet
}

internal fun importItemTypes(
	assets: InventoryAssets, rawSheetIDs: String, rawStackableTypes: String
) {
	val stackableTypes = parseActionScriptObject(rawStackableTypes)
	for ((typeName, rawSheetLocation) in parseActionScriptObject(rawSheetIDs)) {
		val sheetLocation = parseActionScriptNestedList(rawSheetLocation)
		if (sheetLocation !is ArrayList<*> || sheetLocation.size != 2) {
			throw ItemParseException("Unexpected raw sheet location $rawSheetLocation")
		}
		if (sheetLocation[0] == "\"F\"") continue // Fishing rod & bait were never really added in the original game

		val itemType = ItemType(
			flashName = typeName,
			canStack = stackableTypes[typeName] == "1",
		)
		assets.itemTypes.add(itemType)
	}
}
