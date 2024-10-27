package mardek.importer.area

import mardek.assets.area.ParsedTile
import mardek.assets.area.ParsedTilesheet
import mardek.assets.area.WaterType
import java.awt.Color
import javax.imageio.ImageIO

private val BLUE_ENCODING = Color(0, 221, 255).rgb
private val CYAN_ENCODING = Color.CYAN.rgb
private val DARK_BLUE_ENCODING = Color(0, 0, 200).rgb
private val RED_ENCODING = Color(255, 0, 0).rgb
private val GREEN_ENCODING = Color.GREEN.rgb
private val DARK_GREEN_ENCODING = Color(0f, .6f, 0f).rgb

fun parseTilesheet(name: String): ParsedTilesheet {
	val sheetInput = TileSlice::class.java.getResourceAsStream("tilesheets/$name.png")
	val tilesheet = ImageIO.read(sheetInput)
	sheetInput.close()

	fun toTileID(x: Int, y: Int, height: Int) = Integer.parseInt("${1 + x / 10}${((y - 1) / height) * 10 + x % 10}")

	val idMapping = mutableMapOf<Int, ParsedTile>()

	val tileSize = 16
	for (x in 0 until tilesheet.width / tileSize) {
		for (y in 1 until tilesheet.height / tileSize) {
			val tileHeight = 1 + x / 10
			if ((y - 1) % tileHeight != 0) continue
			if ((y + tileHeight) * tileSize > tilesheet.height) continue

			val encodingColor = tilesheet.getRGB(x, y - 1) // TODO Might need to divide y by tileHeight
			val tileID = toTileID(x, y, tileHeight)

			val sprites = (0 until tileHeight).map { layer ->
				tilesheet.getSubimage(tileSize * x, tileSize * (layer + y), tileSize, tileSize)
			}

			val canWalkOn = encodingColor == GREEN_ENCODING || encodingColor == DARK_GREEN_ENCODING
					|| encodingColor == CYAN_ENCODING

			val waterType = when (encodingColor) {
				BLUE_ENCODING -> WaterType.Water
				CYAN_ENCODING -> WaterType.Water
				DARK_GREEN_ENCODING -> WaterType.Water
				RED_ENCODING -> WaterType.Lava
				DARK_BLUE_ENCODING -> WaterType.Waterfall
				else -> WaterType.None
			}
			idMapping[tileID] = ParsedTile(tileID, canWalkOn, waterType, sprites)
		}
	}

	val waterSprites = (0 until 5).map { rawX ->
		val x = when (rawX) {
			0 -> 176
			1 -> 192
			2 -> 160
			else -> 160 + 16 * rawX
		}
		tilesheet.getSubimage(x, 0, tileSize, tileSize)
	}

	return ParsedTilesheet(name, idMapping, waterSprites)
}