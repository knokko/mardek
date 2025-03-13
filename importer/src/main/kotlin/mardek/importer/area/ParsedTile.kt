package mardek.importer.area

import mardek.content.area.WaterType
import java.awt.image.BufferedImage

class ParsedTile(
		val canWalkOn: Boolean, val waterType: WaterType,
		val sprites: List<BufferedImage>, val hexObjectColor: Int
) {

	override fun toString() = "Tile(canWalkOn=$canWalkOn, water=$waterType, #sprites=${sprites.size})"
}
