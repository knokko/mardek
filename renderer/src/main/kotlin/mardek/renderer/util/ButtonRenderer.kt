package mardek.renderer.util

import com.github.knokko.boiler.utilities.ColorPacker.multiplyAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import com.github.knokko.vk2d.text.Vk2dFont
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.ui.Fonts
import mardek.renderer.MardekTextStyles
import mardek.renderer.menu.referenceTime
import mardek.state.util.Rectangle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal fun renderButton(
	colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch, textBatch: Vk2dFancyTextBatch, font: Vk2dFont,
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

	textBatch.drawString(
		text, textOffsetX.toFloat(), textBaseY.toFloat(),
		0f, textHeight.toFloat(), font,
		MardekTextStyles.button(lowerTextColor, upperTextColor, showTextOutline),
		TextAlignment.LEFT,
	)
}

internal fun renderInnerBoxButton(
	colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch,
	simpleTextBatch: Vk2dSimpleTextBatch, fancyTextBatch: Vk2dFancyTextBatch,
	bundle: Vk2dResourceBundle, fonts: Fonts,
	x: Int, boxY: Int, boxSize: Int, borderWidth: Int, boxRadius: Int, cornerDistances: FloatArray,
	boxColor: Int, token: String, label: String,
) {
	val tokenFont = bundle.getFont(fonts.basic2.index)
	val labelFont = bundle.getFont(fonts.large1.index)
	val textColor = srgbToLinear(rgb(186, 146, 77))
	val shadowColor = rgb(0, 0, 0)
	val shadowOffset = boxSize * 0.08f

	colorBatch.fill(
		x + boxRadius, boxY + borderWidth,
		x + boxSize - 1 - boxRadius, boxY + boxSize - 1 - borderWidth,
		boxColor,
	)
	colorBatch.fill(
		x + borderWidth, boxY + boxRadius,
		x + boxRadius - 1, boxY + boxSize - 1 - boxRadius,
		boxColor
	)
	colorBatch.fill(
		x + boxSize - boxRadius, boxY + boxRadius,
		x + boxSize - 1 - borderWidth, boxY + boxSize - 1 - boxRadius,
		boxColor
	)

	val borderColor = srgbToLinear(rgba(73, 52, 37, 150))
	colorBatch.fill(
		x, boxY + boxRadius,
		x + borderWidth - 1, boxY + boxSize - boxRadius - 1,
		borderColor,
	)
	colorBatch.fill(
		x + boxSize - borderWidth, boxY + boxRadius,
		x + boxSize - 1, boxY + boxSize - boxRadius - 1,
		borderColor,
	)
	colorBatch.fill(
		x + boxRadius, boxY,
		x + boxSize - boxRadius - 1, boxY + borderWidth - 1,
		borderColor,
	)
	colorBatch.fill(
		x + boxRadius, boxY + boxSize - borderWidth,
		x + boxSize - boxRadius - 1, boxY + boxSize - 1,
		borderColor,
	)

	val r = boxRadius.toFloat()

	fun renderQuarterOval(minX: Int, minY: Int, maxX: Int, maxY: Int, centerX: Float, centerY: Float) {
		ovalBatch.complex(
			minX, minY, maxX, maxY, centerX, centerY, r, r,
			boxColor, boxColor, borderColor, borderColor, 0,
			cornerDistances[0], cornerDistances[1],
			cornerDistances[2], cornerDistances[3],
		)
	}

	renderQuarterOval(
		x, boxY,x + boxRadius - 1, boxY + boxRadius - 1,
		x + r, boxY + r,
	)
	renderQuarterOval(
		x, boxY + boxSize - boxRadius,x + boxRadius - 1, boxY + boxSize - 1,
		x + r, boxY + boxSize - r,
	)
	renderQuarterOval(
		x + boxSize - boxRadius, boxY,x + boxSize - 1, boxY + boxRadius - 1,
		x + boxSize - r, boxY + r,
	)
	renderQuarterOval(
		x + boxSize - boxRadius - 1, boxY + boxSize - boxRadius,
		x + boxSize - 1, boxY + boxSize - 1,
		x + boxSize - r, boxY + boxSize - r,
	)

	val tokenBaseX = x + boxSize * 0.45f
	val textY = boxY + boxSize * 0.7f
	val textHeight = boxSize * 0.5f
	if (label.isEmpty()) {
		fancyTextBatch.drawString(
			token, tokenBaseX, textY + 0.1f * boxSize, 0f, textHeight, tokenFont,
			MardekTextStyles.Dialogue.SKIP_BUTTON, TextAlignment.CENTERED,
		)
	} else {
		simpleTextBatch.drawShadowedString(
			token, tokenBaseX, textY, textHeight,
			tokenFont, textColor, 0, 0f, shadowColor,
			shadowOffset, TextAlignment.CENTERED,
		)
		simpleTextBatch.drawShadowedString(
			label, x + boxSize * 1.3f, textY, textHeight,
			labelFont, textColor, 0, 0f, shadowColor,
			shadowOffset, TextAlignment.LEFT,
		)
	}
}

internal fun renderBoxButton(
	colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch,
	simpleTextBatch: Vk2dSimpleTextBatch, fancyTextBatch: Vk2dFancyTextBatch,
	bundle: Vk2dResourceBundle, fonts: Fonts,
	minBoxSize: Float, boxX: Int, boxY: Int,
) {
	val boxSizePeriod = 1_000_000_000L
	val relativeTime = ((System.nanoTime() - referenceTime) % boxSizePeriod).toFloat() / boxSizePeriod
	val maxBoxSize = 1.083f * minBoxSize
	val floatBoxSize = minBoxSize + (2f * abs(0.5f - relativeTime)) * (maxBoxSize - minBoxSize)
	val boxSize = floatBoxSize.roundToInt()
	val cornerRadius = (minBoxSize / 6f).roundToInt()
	val darkColor = srgbToLinear(rgb(145, 137, 112))
	val lightColor = srgbToLinear(rgb(167, 161, 141))
	val cornerDistances = floatArrayOf(0.6f, 0.65f, 1f, 1.05f)
	val borderWidth = max(1, boxSize / 15)

	renderInnerBoxButton(
		colorBatch, ovalBatch, simpleTextBatch, fancyTextBatch, bundle, fonts,
		boxX, boxY, boxSize, borderWidth, cornerRadius, cornerDistances,
		darkColor, "E", "",
	)
	colorBatch.fill(
		boxX + 5 * borderWidth / 2, boxY + 4 * borderWidth,
		boxX + boxSize - 1 - 5 * borderWidth / 2, boxY + 5 * boxSize / 9, lightColor
	)
	colorBatch.fill(
		boxX + 4 * borderWidth, boxY + 5 * borderWidth / 2,
		boxX + boxSize - 1 - 4 * borderWidth, boxY + 4 * borderWidth - 1, lightColor
	)

	val radius = borderWidth * 1.5f
	ovalBatch.aliased(
		boxX + 5 * borderWidth / 2, boxY + 5 * borderWidth / 2,
		boxX + 4 * borderWidth - 1, boxY + 4 * borderWidth - 1,
		boxX + 4f * borderWidth, boxY + 4f * borderWidth,
		radius, radius, lightColor,
	)
	ovalBatch.aliased(
		boxX + boxSize - 4 * borderWidth - 1, boxY + 5 * borderWidth / 2,
		boxX + boxSize - 5 * borderWidth / 2 - 1, boxY + 4 * borderWidth - 1,
		boxX + boxSize - 4f * borderWidth, boxY + 4f * borderWidth,
		radius, radius, lightColor,
	)
}
