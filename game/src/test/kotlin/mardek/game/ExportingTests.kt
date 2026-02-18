package mardek.game

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.assertNull

object ExportingTests {

	fun testImageDataWasRemoved(instance: TestingInstance) {
		instance.apply {
			val kimSprite = content.items.items.first().sprite
			assertNull(kimSprite.data)
			assertNotEquals(0, kimSprite.header)
			assertNotEquals(-1, kimSprite.index)

			val bcSprite = content.ui.titleScreenBackground
			assertNull(bcSprite.data)
			assertNotEquals(-1, bcSprite.index)

			val font = content.fonts.basic2
			assertNull(font.data)
			assertNotEquals(-1, font.index)
		}
	}
}
