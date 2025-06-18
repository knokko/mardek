package mardek.renderer.ui

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.text.font.FontData
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.state.title.AbsoluteRectangle

fun renderButton(
	uiRenderer: UiRenderer, font: FontData, showTextOutline: Boolean,
	text: String, isSelected: Boolean, rect: AbsoluteRectangle,
	outlineWidth: Int, textOffsetX: Int, textBaseY: Int, textHeight: Int,
) {
	val borderLight = srgbToLinear(rgb(255, 204, 153))
	val borderHoverLight = srgbToLinear(rgb(152, 190, 222))
	val borderDark = srgbToLinear(rgb(101, 50, 1))
	val borderHoverDark = srgbToLinear(rgb(31, 68, 122))
	val innerDark = srgbToLinear(rgb(169, 71, 6))
	val innerLight = srgbToLinear(rgb(183, 105, 53))

	val innerLeft = srgbToLinear(rgba(169, 67, 1, 204))
	val innerHoverLeft = srgbToLinear(rgba(6, 82, 155, 184))
	val innerRight = srgbToLinear(rgba(68, 45, 33, 108))
	val innerHoverRight = rgba(0, 0, 0, 0)

	val rightAlpha = 200

	fun fillColors(borderTopLeft: Int, borderBottomRight: Int, left: Int, right: Int) {
		uiRenderer.fillColor(rect.minX, rect.minY, rect.maxX, rect.minY + outlineWidth - 1, borderTopLeft)
		uiRenderer.fillColor(rect.minX, rect.minY, rect.minX + outlineWidth - 1, rect.maxY, borderTopLeft)
		uiRenderer.fillColor(
			rect.minX + outlineWidth, rect.minY + outlineWidth, rect.maxX, rect.maxY, borderBottomRight,
			Gradient(0, 0, rect.width - 2 * outlineWidth, rect.height - 2 * outlineWidth, left, right, left)
		)
	}
	fillColors(borderLight, borderDark, innerLeft, innerRight)

	val upperTextColor = srgbToLinear(rgb(248, 232, 194))
	val lowerTextColor = srgbToLinear(rgb(238, 203, 127))

	val outlineColors = if (showTextOutline) intArrayOf(srgbToLinear(rgb(112, 64, 33)))
	else IntArray(0)
	uiRenderer.drawString(
		font, text, lowerTextColor, outlineColors, textOffsetX, rect.minY, rect.maxX, rect.maxY,
		textBaseY, textHeight, 1, TextAlignment.DEFAULT,
		Gradient(0, 0, rect.width, rect.height / 2, upperTextColor, upperTextColor, upperTextColor)
	)

	if (isSelected) {
		fillColors(borderHoverDark, borderHoverLight, innerHoverLeft, innerHoverRight)
	}
}
