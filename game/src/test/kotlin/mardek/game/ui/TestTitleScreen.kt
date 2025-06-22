package mardek.game.ui

import mardek.game.TestingInstance
import mardek.game.testRendering
import mardek.renderer.SharedResources
import mardek.state.title.TitleScreenState
import java.awt.Color
import java.util.concurrent.CompletableFuture

object TestTitleScreen {

	fun testRendering(instance: TestingInstance) {
		instance.apply {
			val getResources = CompletableFuture<SharedResources>()
			getResources.complete(SharedResources(getBoiler, 1, skipWindow = true))

			val expectedColors = arrayOf(
				Color(255, 255, 255), // Sun color
				Color(242, 183, 113), // Subtitle color
				Color(255, 204, 153), // Button border color
			)

			val state = TitleScreenState()
			testRendering(getResources, state, 800, 450, "title-screen", expectedColors, emptyArray())

			getResources.join().destroy()
		}
	}
}
