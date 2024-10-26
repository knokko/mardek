package mardek.importer.area

import mardek.assets.area.WaterType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class TestTilesheetParser {

	private fun assertImageEquals(expected: BufferedImage, actual: BufferedImage) {
		assertEquals(expected.width, actual.width)
		assertEquals(expected.height, actual.height)

		for (y in 0 until expected.height) {
			for (x in 0 until expected.width) {
				assertEquals(expected.getRGB(x, y), actual.getRGB(x, y))
			}
		}
	}

	@Test
	fun testAeropolis() {
		val parsed = parseTilesheet("aeropolis")
		val fullImageInput = TestTilesheetParser::class.java.getResourceAsStream("tilesheets/aeropolis.png")!!
		val fullImage = ImageIO.read(fullImageInput)
		fullImageInput.close()

		assertImageEquals(fullImage.getSubimage(176, 0, 16, 16), parsed.waterSprites[0])
		assertImageEquals(fullImage.getSubimage(192, 0, 16, 16), parsed.waterSprites[1])
		assertImageEquals(fullImage.getSubimage(160, 0, 16, 16), parsed.waterSprites[2])

		assertImageEquals(fullImage.getSubimage(0, 16, 16, 16), parsed.tiles[10]!!.sprites[0])
		assertTrue(parsed.tiles[10]!!.canWalkOn)
		assertEquals(WaterType.None, parsed.tiles[10]!!.waterType)

		assertImageEquals(fullImage.getSubimage(192, 16, 16, 16), parsed.tiles[22]!!.sprites[0])
		assertImageEquals(fullImage.getSubimage(192, 32, 16, 16), parsed.tiles[22]!!.sprites[1])
		assertFalse(parsed.tiles[22]!!.canWalkOn)
		assertEquals(WaterType.None, parsed.tiles[22]!!.waterType)

		assertImageEquals(fullImage.getSubimage(48, 80, 16, 16), parsed.tiles[143]!!.sprites[0])

		assertImageEquals(fullImage.getSubimage(96, 176, 16, 16), parsed.tiles[1106]!!.sprites[0])
		assertFalse(parsed.tiles[1106]!!.canWalkOn)
		assertEquals(WaterType.Water, parsed.tiles[1106]!!.waterType)

		assertImageEquals(fullImage.getSubimage(336, 16, 16, 16), parsed.tiles[31]!!.sprites[0])
		assertImageEquals(fullImage.getSubimage(336, 48, 16, 16), parsed.tiles[31]!!.sprites[2])
	}
}
