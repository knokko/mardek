package mardek.importer.area

import java.awt.image.BufferedImage

class ParsedTilesheet(
		val name: String,
		val tiles: Map<Int, ParsedTile>,
		val waterSprites: List<BufferedImage>
) {
	init {
		if (waterSprites.size != 5) throw IllegalArgumentException("There must be 5 water sprites")
		for (sprite in waterSprites + tiles.flatMap { it.value.sprites }) {
			if (sprite.width != 16 || sprite.height != 16) {
				throw IllegalArgumentException("All sprite dimensions must be 16")
			}
		}
	}
}
