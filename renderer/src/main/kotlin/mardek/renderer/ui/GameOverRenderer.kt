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
		context.uiRenderer.begin(context.recorder, context.targetImage)
		context.uiRenderer.beginBatch()

		val screenWidth = context.targetImage.width
		val screenHeight = context.targetImage.height
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
			0, 0, context.targetImage.width, context.targetImage.height,
			2 * context.targetImage.height / 3, context.targetImage.height / 40,
			1, TextAlignment.CENTER
		)

		val timeSinceGameOver = System.nanoTime() - state.startTime
		val fade = max(0L, 255L - 255L * timeSinceGameOver / 5000_000_000L).toInt()
		if (fade > 0) {
			context.uiRenderer.fillColor(
				0, 0, context.targetImage.width, context.targetImage.height,
				rgba(0, 0, 0, fade)
			)
		}

		context.uiRenderer.endBatch()
		context.uiRenderer.end()
	}
}
