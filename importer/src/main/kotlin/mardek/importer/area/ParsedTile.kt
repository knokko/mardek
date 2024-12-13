package mardek.importer.area

import mardek.assets.area.WaterType
import java.awt.image.BufferedImage

class ParsedTile(
		val id: Int, val canWalkOn: Boolean, val waterType: WaterType,
		val sprites: List<BufferedImage>, val hexObjectColor: Int
) {

	override fun toString() = "Tile(id=$id, canWalkOn=$canWalkOn, water=$waterType, #sprites=${sprites.size})"

	override fun equals(other: Any?) = other is ParsedTile && id == other.id && canWalkOn == other.canWalkOn &&
			waterType == other.waterType && sprites.size == other.sprites.size

	override fun hashCode() = id
}
