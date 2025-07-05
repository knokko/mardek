package mardek.renderer

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.renderer.batch.RectangleRenderer
import mardek.state.GameStateManager
import mardek.state.title.AbsoluteRectangle
import org.lwjgl.vulkan.VK10.vkCmdSetScissor
import org.lwjgl.vulkan.VK10.vkCmdSetViewport
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkViewport

internal fun renderTitleBar(
	recorder: CommandRecorder, targetImage: VkbImage, rectangles: RectangleRenderer,
	pScissor: VkRect2D.Buffer, state: GameStateManager
) {
	recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)

	val borderColor = srgbToLinear(rgb(74, 58, 48))
	val iconColor = srgbToLinear(rgb(132, 105, 82))
	val hoverColor = srgbToLinear(rgb(242, 183, 113))
	rectangles.beginBatch(
		recorder, targetImage.width, targetImage.height, 6
	)
	rectangles.fill(
		BORDER_WIDTH, BORDER_WIDTH,
		targetImage.width - 1 - BORDER_WIDTH,
		pScissor.offset().y() - 1, borderColor
	)

	val crossX = targetImage.width - 30
	val crossY = 6
	val crossLength = 16
	val crossWidth = 3
	val crossLocation = AbsoluteRectangle(crossX, crossY, crossLength, crossLength)
	val crossColor = if (state.hoveringCross) hoverColor else iconColor
	rectangles.fillUnaligned(
		crossX + crossWidth, crossY,
		crossX, crossY + crossWidth,
		crossX + crossLength - crossWidth, crossY+ crossLength,
		crossX + crossLength, crossY + crossLength - crossWidth,
		crossColor
	)
	rectangles.fillUnaligned(
		crossX, crossY + crossLength - crossWidth,
		crossX + crossWidth, crossY + crossLength,
		crossX + crossLength, crossY + crossWidth,
		crossX + crossLength - crossWidth, crossY,
		crossColor
	)
	state.crossLocation = crossLocation

	val maximizeX = targetImage.width - 55
	val maximizeGap = 2
	rectangles.fill(
		maximizeX + maximizeGap, crossY + maximizeGap,
		maximizeX + crossLength - maximizeGap, crossY + crossLength - maximizeGap,
		if (state.hoveringMaximize) hoverColor else iconColor
	)
	rectangles.fill(
		maximizeX + maximizeGap + crossWidth, crossY + maximizeGap + crossWidth,
		maximizeX + crossLength - maximizeGap - crossWidth, crossY + crossLength - maximizeGap - crossWidth,
		borderColor
	)
	state.maximizeLocation = AbsoluteRectangle(maximizeX, crossY, crossLength, crossLength)

	val minusX = targetImage.width - 80
	val minusY = crossY + (crossLength - crossWidth) / 2
	val minusLocation = AbsoluteRectangle(minusX, crossY, crossLength, crossLength)
	rectangles.fill(
		minusX, minusY, minusLocation.maxX, minusY + crossWidth,
		if (state.hoveringMinus) hoverColor else iconColor
	)
	state.minusLocation = minusLocation

	rectangles.endBatch(recorder)

	val pViewport = VkViewport.calloc(1, recorder.stack)
	pViewport.x(pScissor.offset().x().toFloat())
	pViewport.y(pScissor.offset().y().toFloat())
	pViewport.width(pScissor.extent().width().toFloat())
	pViewport.height(pScissor.extent().height().toFloat())
	pViewport.minDepth(0f)
	pViewport.maxDepth(1f)

	vkCmdSetViewport(recorder.commandBuffer, 0, pViewport)
	vkCmdSetScissor(recorder.commandBuffer, 0, pScissor)
}
