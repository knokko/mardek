package mardek.game.ui

import mardek.game.TestingInstance
import mardek.game.testRendering
import mardek.state.title.TitleScreenState
import java.awt.Color

object TestTitleScreen {

	fun testRendering(instance: TestingInstance) {
		instance.apply {
			val expectedColors = arrayOf(
				Color(255, 255, 255), // Sun color
				Color(190, 144, 95), // Title outer outline
				Color(69, 50, 34), // Title inner outline
				Color(242, 183, 113), // Subtitle upper color
				Color(184, 130, 61), // Subtitle lower color
				Color(255, 204, 153), // Button border color
			)

			val state = TitleScreenState()
			testRendering(state, 800, 450, "title-screen", expectedColors, emptyArray())
		}
	}
}
