package mardek.importer.area

import mardek.assets.area.Area
import mardek.assets.area.Tile
import mardek.assets.area.TileAnimationFrame
import java.awt.Color
import java.lang.Integer.parseInt
import java.util.Scanner
import javax.imageio.ImageIO

private val CYAN_ENCODING = Color.CYAN.rgb
private val GREEN_ENCODING = Color.GREEN.rgb
private val DARK_GREEN_ENCODING = Color(0f, .6f, 0f).rgb

class TileSlice(val x: Int, val y: Int, val encoding: Int, val height: Int) {
	var index = -1

	override fun toString() = "TileSlice($x, $y, height=$height, encoding=$encoding, index=$index)"
}

fun importArea(areaName: String): Area {
	var tilesheetName = ""
	var tileIDs: IntArray? = null
	var width = 0
	var height = 0
	val scanner = Scanner(TileSlice::class.java.getResourceAsStream("data/$areaName.txt"))
	while (scanner.hasNextLine()) {
		val line = scanner.nextLine().replace(" ", "")
		if (line.startsWith("map=")) {
			val startIndex = line.indexOf('[') + 2
			val endIndex = line.lastIndexOf(']') - 1
			val rows = line.substring(startIndex, endIndex).split("],[")
			val columns = rows.map { row -> row.split(",").map { parseInt(it) } }
			height = rows.size
			width = columns[0].size
			for (colum in columns) {
				if (colum.size != width) throw IllegalArgumentException("Not all columns have the same size")
			}
			tileIDs = IntArray(width * height) { index -> columns[index / width][index % width] }
		}
		if (line.startsWith("tileset=")) {
			tilesheetName = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'))
		}
	}
	scanner.close()

	val sheetInput = TileSlice::class.java.getResourceAsStream("tilesheets/$tilesheetName.png")
	val tilesheet = ImageIO.read(sheetInput)
	sheetInput.close()

	fun toTileID(x: Int, y: Int, height: Int) = parseInt("${1 + x / 10}${((y - 1) / height) * 10 + x % 10}")

	val idMapping = mutableMapOf<Int, TileSlice>()

	val tileSize = 16
	for (x in 0 until tilesheet.width / tileSize) {
		for (y in 1 until tilesheet.height / tileSize) {
			val tileHeight = 1 + x / 10
			if ((y - 1) % tileHeight != 0) continue
			if ((y + tileHeight) * tileSize > tilesheet.height) continue

			val encodingColor = tilesheet.getRGB(x, y - 1)
			val tileID = toTileID(x, y, tileHeight)
			idMapping[tileID] = TileSlice(x, y, encodingColor, tileHeight)
		}
	}

	val tileList = ArrayList<Tile>(idMapping.size)

	var nextTileIndex = 0
	for (tileSlice in idMapping.values) {
		val images = (0 until tileSlice.height).map { layer ->
				tilesheet.getSubimage(tileSize * tileSlice.x, tileSize * (layer + tileSlice.y), tileSize, tileSize)
		}

		val canMoveTo = tileSlice.encoding == GREEN_ENCODING || tileSlice.encoding == DARK_GREEN_ENCODING
				|| tileSlice.encoding == CYAN_ENCODING

		// TODO Animation support
		val tile = Tile(canMoveTo, animations = listOf(TileAnimationFrame(images)))
		tileSlice.index = nextTileIndex
		tileList.add(tile)
		nextTileIndex += 1
	}

	val tileIndices = tileIDs!!.map { idMapping[it]!!.index }.toIntArray()

	return Area(areaName, tilesheetName, width, height, tileIndices, tileList.toList())
}