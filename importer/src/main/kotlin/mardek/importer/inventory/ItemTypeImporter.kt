package mardek.importer.inventory

import mardek.assets.inventory.InventoryAssets
import mardek.assets.inventory.ItemType
import mardek.importer.area.parseFlashString
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import java.awt.image.BufferedImage
import java.lang.Integer.parseInt
import javax.imageio.ImageIO

private fun sheet(type: String): BufferedImage {
	val input = ItemParseException::class.java.getResourceAsStream("itemsheet_$type.png")
		?: throw ItemParseException("Can't find sheet $type")
	val sheet = ImageIO.read(input)
	input.close()
	return sheet
}
internal fun importItemTypes(
	assets: InventoryAssets, rawSheetIDs: String, rawStackableTypes: String
): Map<ItemType, List<BufferedImage>> {
	val sheetMap = mutableMapOf(
		Pair("A", sheet("armour")),
		Pair("M", sheet("misc")),
		Pair("W", sheet("weapons")),
	)
	val spriteMapping = mutableMapOf<ItemType, List<BufferedImage>>()
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

		val sheet = sheetMap[parseFlashString(sheetLocation[0] as String, "Sheet for $rawSheetLocation")] ?:
			throw ItemParseException("Failed to import $rawSheetLocation")
		val sheetRow = sheet.getSubimage(0, 16 * parseInt(sheetLocation[1] as String), sheet.width, 16)
		spriteMapping[itemType] = (0 until sheet.width / 16).map {
			sheetRow.getSubimage(16 * it, 0, 16, 16)
		}
	}

	return spriteMapping
}
