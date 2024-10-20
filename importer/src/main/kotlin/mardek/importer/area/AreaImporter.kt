package mardek.importer.area

import mardek.assets.area.Area
import mardek.assets.area.Tile
import java.awt.Color
import java.lang.Integer.parseInt
import java.util.Scanner
import javax.imageio.ImageIO

private val BLUE_ENCODING = Color(0, 221, 255).rgb
private val CYAN_ENCODING = Color.CYAN.rgb
private val DARK_BLUE_ENCODING = Color(0, 0, 200).rgb
private val RED_ENCODING = Color(255, 0, 0).rgb
private val GREEN_ENCODING = Color.GREEN.rgb
private val DARK_GREEN_ENCODING = Color(0f, .6f, 0f).rgb

class TileSlice(val x: Int, val y: Int, val encoding: Int, val height: Int) {
	var index = -1

	override fun toString() = "TileSlice($x, $y, height=$height, encoding=$encoding, index=$index)"

	fun getWaterType() = when (encoding) {
		BLUE_ENCODING -> 2
		CYAN_ENCODING -> 2
		DARK_GREEN_ENCODING -> 2
		RED_ENCODING -> 3
		DARK_BLUE_ENCODING -> 4
		else -> 0
	}
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

		val tile = Tile(canMoveTo, images)
		tileSlice.index = nextTileIndex
		tileList.add(tile)
		nextTileIndex += 1
	}

	val tileIndices = tileIDs!!.map { idMapping[it]!!.index }.toIntArray()

	val waterSprites = (0 until 5).map { rawX ->
		val x = when (rawX) {
			0 -> 176
			1 -> 192
			2 -> 160
			else -> 160 + 16 * rawX
		}
		tilesheet.getSubimage(x, 0, tileSize, tileSize)
	}

	val waterGrid = IntArray(width * height) { index -> idMapping[tileIDs[index]]!!.getWaterType() }

	return Area(areaName, tilesheetName, width, height, tileIndices, tileList.toList(), waterGrid, waterSprites)
}
