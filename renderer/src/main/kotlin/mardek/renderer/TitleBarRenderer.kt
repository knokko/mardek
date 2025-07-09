package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dFont
import mardek.state.GameStateManager
import mardek.state.util.Rectangle

internal fun renderTitleBar(
	state: GameStateManager, colorBatch: Vk2dColorBatch,
	textBatch: Vk2dGlyphBatch, font: Vk2dFont, fps: Long?,
) {
	val borderColor = srgbToLinear(rgb(74, 58, 48))
	val iconColor = srgbToLinear(rgb(132, 105, 82))
	val hoverColor = srgbToLinear(rgb(242, 183, 113))

	colorBatch.fill(
		BORDER_WIDTH, BORDER_WIDTH,
		colorBatch.width - 1 - BORDER_WIDTH,
		FULL_BORDER_HEIGHT - 1, borderColor
	)

	val crossX = colorBatch.width - 30
	val crossY = 6
	val crossLength = 16
	val crossWidth = 3
	val crossLocation = Rectangle(crossX, crossY, crossLength, crossLength)
	val crossColor = if (state.hoveringCross) hoverColor else iconColor
	colorBatch.fillUnaligned(
		crossX + crossWidth, crossY,
		crossX, crossY + crossWidth,
		crossX + crossLength - crossWidth, crossY+ crossLength,
		crossX + crossLength, crossY + crossLength - crossWidth,
		crossColor
	)
	colorBatch.fillUnaligned(
		crossX, crossY + crossLength - crossWidth,
		crossX + crossWidth, crossY + crossLength,
		crossX + crossLength, crossY + crossWidth,
		crossX + crossLength - crossWidth, crossY,
		crossColor
	)
	state.crossLocation = crossLocation

	val maximizeX = colorBatch.width - 55
	val maximizeGap = 2
	colorBatch.fill(
		maximizeX + maximizeGap, crossY + maximizeGap,
		maximizeX + crossLength - maximizeGap, crossY + crossLength - maximizeGap,
		if (state.hoveringMaximize) hoverColor else iconColor
	)
	colorBatch.fill(
		maximizeX + maximizeGap + crossWidth, crossY + maximizeGap + crossWidth,
		maximizeX + crossLength - maximizeGap - crossWidth, crossY + crossLength - maximizeGap - crossWidth,
		borderColor
	)
	state.maximizeLocation = Rectangle(maximizeX, crossY, crossLength, crossLength)

	val minusX = colorBatch.width - 80
	val minusY = crossY + (crossLength - crossWidth) / 2
	val minusLocation = Rectangle(minusX, crossY, crossLength, crossLength)
	colorBatch.fill(
		minusX, minusY, minusLocation.maxX, minusY + crossWidth,
		if (state.hoveringMinus) hoverColor else iconColor
	)
	state.minusLocation = minusLocation

	if (fps != null) {
		textBatch.drawString(
			"$fps FPS", minusX - 20f, 0.75f * FULL_BORDER_HEIGHT,
			0.5f * FULL_BORDER_HEIGHT, font, hoverColor, TextAlignment.RIGHT,
		)
	}
}
