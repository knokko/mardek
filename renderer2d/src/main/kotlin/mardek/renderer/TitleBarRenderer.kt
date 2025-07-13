package mardek.renderer

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.state.util.Rectangle

internal fun renderTitleBar(context: RawRenderContext, batch: Vk2dColorBatch) {
	val borderColor = srgbToLinear(rgb(74, 58, 48))
	val iconColor = srgbToLinear(rgb(132, 105, 82))
	val hoverColor = srgbToLinear(rgb(242, 183, 113))

	batch.fill(
		BORDER_WIDTH, BORDER_WIDTH,
		batch.width - 1 - BORDER_WIDTH,
		FULL_BORDER_HEIGHT - 1, borderColor
	)

	val crossX = batch.width - 30
	val crossY = 6
	val crossLength = 16
	val crossWidth = 3
	val crossLocation = Rectangle(crossX, crossY, crossLength, crossLength)
	val crossColor = if (context.state.hoveringCross) hoverColor else iconColor
	batch.fillUnaligned(
		crossX + crossWidth, crossY,
		crossX, crossY + crossWidth,
		crossX + crossLength - crossWidth, crossY+ crossLength,
		crossX + crossLength, crossY + crossLength - crossWidth,
		crossColor
	)
	batch.fillUnaligned(
		crossX, crossY + crossLength - crossWidth,
		crossX + crossWidth, crossY + crossLength,
		crossX + crossLength, crossY + crossWidth,
		crossX + crossLength - crossWidth, crossY,
		crossColor
	)
	context.state.crossLocation = crossLocation

	val maximizeX = batch.width - 55
	val maximizeGap = 2
	batch.fill(
		maximizeX + maximizeGap, crossY + maximizeGap,
		maximizeX + crossLength - maximizeGap, crossY + crossLength - maximizeGap,
		if (context.state.hoveringMaximize) hoverColor else iconColor
	)
	batch.fill(
		maximizeX + maximizeGap + crossWidth, crossY + maximizeGap + crossWidth,
		maximizeX + crossLength - maximizeGap - crossWidth, crossY + crossLength - maximizeGap - crossWidth,
		borderColor
	)
	context.state.maximizeLocation = Rectangle(maximizeX, crossY, crossLength, crossLength)

	val minusX = batch.width - 80
	val minusY = crossY + (crossLength - crossWidth) / 2
	val minusLocation = Rectangle(minusX, crossY, crossLength, crossLength)
	batch.fill(
		minusX, minusY, minusLocation.maxX, minusY + crossWidth,
		if (context.state.hoveringMinus) hoverColor else iconColor
	)
	context.state.minusLocation = minusLocation
}
