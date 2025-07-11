package mardek.renderer.ui

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.CircleGradient
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.RenderContext
import mardek.renderer.StateRenderer
import mardek.state.title.GameOverState
import kotlin.math.max

class GameOverRenderer(
	private val state: GameOverState
): StateRenderer() {

	override fun render(context: RenderContext) {
		context.uiRenderer.begin(
			context.recorder, context.viewportWidth, context.viewportHeight
		)
		context.uiRenderer.beginBatch()

		val screenWidth = context.viewportWidth
		val screenHeight = context.viewportHeight
		context.uiRenderer.fillCircle(
			screenWidth / 5, screenHeight / 3,
			4 * screenWidth / 5, 2 * screenHeight / 3,
			0, CircleGradient(
				0f, 1f, srgbToLinear(rgb(80, 0, 0)), 0
			)
		)

		val titleHeight = screenHeight / 12
		val titleColor2 = srgbToLinear(rgb(194, 1, 10))
		val titleColor3 = srgbToLinear(rgb(224, 49, 45))
		context.uiRenderer.drawString(
			context.resources.font, "GAME OVER",
			srgbToLinear(rgb(138, 0, 3)), IntArray(0),
			0, 0, screenWidth, screenHeight,
			screenHeight / 2 + titleHeight / 2, titleHeight, 1, TextAlignment.CENTER,
			Gradient(
				0, screenHeight / 2 - titleHeight / 3, screenWidth, 3 * titleHeight / 5,
				titleColor2, titleColor2, titleColor2
			),
			Gradient(
				0, screenHeight / 2 - titleHeight / 10, screenWidth, titleHeight / 5,
				titleColor3, titleColor3, titleColor3
			)
		)

		context.uiRenderer.drawString(
			context.resources.font, "Press E or Q to return to the Title Screen",
			srgbToLinear(rgb(220, 70, 70)), IntArray(0),
			0, 0, context.viewportWidth, context.viewportHeight,
			2 * context.viewportHeight / 3, context.viewportHeight / 40,
			1, TextAlignment.CENTER
		)

		context.uiRenderer.endBatch()
		context.uiRenderer.end()

		val timeSinceGameOver = System.nanoTime() - state.startTime
		val fade = max(0L, 255L - 255L * timeSinceGameOver / 5000_000_000L).toInt()
		if (fade > 0) {
			val rectangles = context.resources.rectangleRenderer
			rectangles.beginBatch(context, 1)
			rectangles.fill(
				0, 0, context.viewportWidth, context.viewportHeight,
				rgba(0, 0, 0, fade)
			)
			rectangles.endBatch(context.recorder)
		}
	}
}
