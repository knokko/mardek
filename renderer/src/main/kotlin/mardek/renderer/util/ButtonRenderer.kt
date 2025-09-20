package mardek.renderer.util

import com.github.knokko.boiler.utilities.ColorPacker.multiplyAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dFont
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.util.Rectangle

fun renderButton(
	colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch, glyphBatch: MardekGlyphBatch, font: Vk2dFont,
	showTextOutline: Boolean, text: String, renderLeftBorder: Boolean, isSelected: Boolean, isDisabled: Boolean,
	rect: Rectangle, outlineWidth: Int, textOffsetX: Int, textBaseY: Int, textHeight: Int,
) {
	val disabledAlpha = 0.2f

	var borderLight = srgbToLinear(rgb(255, 204, 153))
	val borderHoverLight = srgbToLinear(rgb(152, 190, 222))

	var borderDark = srgbToLinear(rgb(101, 50, 1))
	val borderHoverDark = srgbToLinear(rgb(31, 68, 122))
	var innerLightLeft = srgbToLinear(rgba(189, 111, 62, 213))
	val innerLightHoverLeft = srgbToLinear(rgba(70, 126, 181, 197))
	var innerLightRight = srgbToLinear(rgba(152, 115, 94, 166))
	val innerLightHoverRight = srgbToLinear(rgba(147, 152, 224, 106))

	var innerLeft = srgbToLinear(rgba(169, 67, 1, 204))
	val innerHoverLeft = srgbToLinear(rgba(6, 82, 155, 182))
	var innerRight = srgbToLinear(rgba(104, 52, 22, 142))
	val innerHoverRight = rgba(46, 54, 193, 66)

	if (isSelected) {
		borderLight = borderHoverLight
		borderDark = borderHoverDark
		innerLeft = innerHoverLeft
		innerRight = innerHoverRight
		innerLightLeft = innerLightHoverLeft
		innerLightRight = innerLightHoverRight
	}

	if (isDisabled) {
		borderLight = multiplyAlpha(borderLight, disabledAlpha)
		borderDark = multiplyAlpha(borderDark, disabledAlpha)
		innerLeft = multiplyAlpha(innerLeft, disabledAlpha)
		innerRight = multiplyAlpha(innerRight, disabledAlpha)
		innerLightLeft = multiplyAlpha(innerLightLeft, disabledAlpha)
		innerLightRight = multiplyAlpha(innerLightRight, disabledAlpha)
	}

	var minX = rect.minX
	if (renderLeftBorder) minX += 2 * outlineWidth

	colorBatch.fill(
		minX, rect.minY,
		rect.maxX - 2 * outlineWidth, rect.minY + outlineWidth - 1, borderLight
	)
	colorBatch.gradient(
		minX, rect.minY + outlineWidth,
		rect.maxX - 2 * outlineWidth, rect.minY + 2 * outlineWidth - 1,
		innerLeft, innerRight, innerLeft
	)
	colorBatch.gradient(
		minX, rect.minY + 2 * outlineWidth,
		rect.maxX - 2 * outlineWidth, rect.minY + rect.height / 2,
		innerLightLeft, innerLightRight, innerLightLeft
	)
	colorBatch.gradient(
		minX, rect.minY + rect.height / 2,
		rect.maxX - 2 * outlineWidth, rect.maxY - outlineWidth,
		innerLeft, innerRight, innerLeft
	)
	colorBatch.fill(
		minX, rect.maxY + 1 - outlineWidth,
		rect.maxX - 2 * outlineWidth, rect.maxY, borderDark
	)
	if (renderLeftBorder) {
		ovalBatch.complex(
			rect.minX, rect.minY, rect.minX + 2 * outlineWidth - 1, rect.maxY,
			rect.minX + 4f * outlineWidth, rect.minY + rect.height / 2f,
			4f * outlineWidth, 0.5f * rect.height + outlineWidth,
			innerLeft, innerLeft, borderLight, borderLight, 0,
			0.86f, 0.86f, 1f, 1f
		)
	}
	ovalBatch.complex(
		rect.maxX + 1 - 2 * outlineWidth, rect.minY, rect.maxX, rect.maxY,
		rect.maxX - 4f * outlineWidth, rect.minY + rect.height / 2f,
		4f * outlineWidth, 0.5f * rect.height + outlineWidth,
		innerRight, innerRight, borderDark, borderDark, 0,
		0.86f, 0.86f, 1f, 1f
	)

	var upperTextColor = srgbToLinear(rgb(248, 232, 194))
	var lowerTextColor = srgbToLinear(rgb(238, 203, 127))
	if (isSelected) {
		upperTextColor = srgbToLinear(rgb(221, 238, 254))
		lowerTextColor = srgbToLinear(rgb(164, 204, 253))
	}

	if (isDisabled) {
		upperTextColor = srgbToLinear(rgb(102, 74, 48))
		lowerTextColor = srgbToLinear(rgb(97, 68, 44))
	}

	val outlineColor = srgbToLinear(rgb(112, 64, 33))
	val outlineWidth = if (showTextOutline) 0.03f * textHeight else 0f

	glyphBatch.drawFancyString(
		text, textOffsetX.toFloat(), textBaseY.toFloat(), textHeight.toFloat(),
		font, lowerTextColor,
		outlineColor, outlineWidth, TextAlignment.LEFT,
		lowerTextColor, upperTextColor, upperTextColor, upperTextColor,
		0.5f, 0.5f, 0.5f, 0.5f
	)
}
