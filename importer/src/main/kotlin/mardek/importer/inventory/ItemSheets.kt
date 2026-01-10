package mardek.importer.inventory

import java.awt.image.BufferedImage

class ItemSheets {
	val mapping = mapOf(
		Pair("misc", ItemSheet("misc")),
		Pair("weapons", ItemSheet("weapons")),
		Pair("armour", ItemSheet("armour"))
	)
}

class ItemSheet(val type: String) {
	val sheet = sheet(type)

	val nextXs = IntArray(sheet.height / 16)

	fun getNext(row: Int): BufferedImage {
		val x = nextXs[row]
		nextXs[row] += 16
		return sheet.getSubimage(x, 16 * row, 16, 16)
	}
}
