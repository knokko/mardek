package mardek.renderer.area.ui.shop

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.state.ingame.actions.PendingSell
import mardek.state.ingame.actions.ShopInteractionState
import mardek.state.util.Rectangle
import kotlin.math.max

internal fun renderShopTradeOverlay(
	context: RenderContext, state: ShopInteractionState, region: Rectangle
) {
	val pendingTrade = state.pendingTrade ?: return

	fun blurColor(shine: Float, alpha: Float) = srgbToLinear(
		rgba(0.4f * shine, 0.25f * shine, 0.17f * shine, alpha)
	)

	val colorBatch = context.addColorBatch(50)
	colorBatch.gradient(
		region.minX, region.minY, region.maxX, region.maxY,
		blurColor(0.2f, 0.98f),
		blurColor(0.7f, 0.96f),
		blurColor(0.2f, 0.98f),
	)

	val itemScale = region.height / 150
	if (itemScale < 1) return

	val boxWidth = 130 * itemScale
	val boxHeight = 40 * itemScale
	val boxRegion = Rectangle(
		region.minX + (region.width - boxWidth) / 2,
		region.minY + (region.height - boxHeight) / 2,
		boxWidth, boxHeight,
	)

	val outerLineWidth = max(1, region.height / 500)
	val outerLineColor = srgbToLinear(rgb(208, 193, 142))
	colorBatch.fill(
		boxRegion.minX, boxRegion.minY, boxRegion.maxX,
		boxRegion.minY + outerLineWidth - 1, outerLineColor,
	)
	colorBatch.fill(
		boxRegion.minX, boxRegion.boundY - outerLineWidth,
		boxRegion.maxX, boxRegion.maxY, outerLineColor,
	)
	colorBatch.fill(
		boxRegion.minX, boxRegion.minY,
		boxRegion.minX + outerLineWidth - 1,
		boxRegion.maxY, outerLineColor,
	)
	colorBatch.fill(
		boxRegion.boundX - outerLineWidth, boxRegion.minY,
		boxRegion.maxX, boxRegion.maxY, outerLineColor,
	)

	val leftGradientColor = srgbToLinear(rgb(27, 16, 11))
	val rightGradientColor = srgbToLinear(rgb(108, 89, 51))
	colorBatch.gradient(
		boxRegion.minX + outerLineWidth, boxRegion.minY + outerLineWidth,
		boxRegion.maxX - outerLineWidth, boxRegion.maxY - outerLineWidth,
		leftGradientColor, rightGradientColor, leftGradientColor,
	)

	val margin = boxRegion.height / 15
	val leftGradientColor2 = srgbToLinear(rgb(60, 48, 42))
	val rightGradientColor2 = srgbToLinear(rgb(127, 109, 76))
	colorBatch.gradientUnaligned(
		boxRegion.minX + margin, boxRegion.maxY - margin,
		changeAlpha(leftGradientColor2, 0),
		boxRegion.maxX - margin, boxRegion.minY + boxRegion.height / 2,
		changeAlpha(rightGradientColor2, 150),
		boxRegion.maxX - margin, boxRegion.minY + margin, rightGradientColor2,
		boxRegion.minX + margin, boxRegion.minY + margin, leftGradientColor,
	)

	val item = pendingTrade.item(
		context.campaign.cursorItemStack,
		context.campaign.shops.get(state.shop),
	)
	context.addKim3Batch(2).simple(
		boxRegion.minX + 8 * itemScale,
		boxRegion.minY + 10 * itemScale,
		itemScale, item.sprite.index,
	)

	val textBatch = context.addFancyTextBatch(400)
	val simpleFont = context.bundle.getFont(context.content.fonts.basic2.index)
	val weakTextColor = srgbToLinear(rgb(207, 192, 141))
	val highlightTextColor = srgbToLinear(rgb(255, 203, 102))
	val strongTextColor = srgbToLinear(rgb(238, 203, 127))

	textBatch.drawString(
		if (pendingTrade is PendingSell) "Sell" else "Buy", boxRegion.minX + 30f * itemScale,
		boxRegion.minY + 11f * itemScale, 0.1f * boxRegion.height, simpleFont, weakTextColor,
	)
	textBatch.drawShadowedString(
		item.displayName, boxRegion.minX + 30f * itemScale, boxRegion.minY + 20f * itemScale,
		0.11f * boxRegion.height, simpleFont, strongTextColor, 0, 0f,
		rgb(0, 0, 0), 0.01f * boxRegion.height,
		0.01f * boxRegion.height, TextAlignment.LEFT,
	)
	colorBatch.fill(
		boxRegion.minX + 50 * itemScale, boxRegion.minY + 25 * itemScale,
		boxRegion.minX + 70 * itemScale, boxRegion.minY + 35 * itemScale,
		srgbToLinear(rgb(44, 32, 20)),
	)

	val numberFont = context.bundle.getFont(context.content.fonts.large1.index)
	textBatch.drawShadowedString(
		pendingTrade.amount.toString(), boxRegion.minX + 55f * itemScale,
		boxRegion.minY + 33f * itemScale, 0.15f * boxRegion.height,
		numberFont, strongTextColor, 0, 0f,
		srgbToLinear(rgb(61, 35, 18)),
		0.025f * boxRegion.height, 0.025f * boxRegion.height,
		TextAlignment.LEFT,
	)

	val imageBatch = context.addImageBatch(4)
	val arrow = context.content.ui.arrowHead
	imageBatch.rotated(
		boxRegion.minX + 45f * itemScale, boxRegion.minY + 30f * itemScale,
		180f, 7f * itemScale / arrow.height, arrow.index, 0, -1,
	)
	imageBatch.rotated(
		boxRegion.minX + 75f * itemScale, boxRegion.minY + 30f * itemScale,
		0f, 7f * itemScale / arrow.height, arrow.index, 0, -1,
	)

	textBatch.drawString(
		"for", boxRegion.minX + 80f * itemScale, boxRegion.minY + 29f * itemScale,
		0.1f * boxRegion.height, simpleFont, weakTextColor,
	)

	var itemValue = item.cost
	if (pendingTrade is PendingSell) itemValue /= 2
	val totalValue = pendingTrade.amount * itemValue
	textBatch.drawShadowedString(
		totalValue.toString(), boxRegion.minX + 90f * itemScale,
		boxRegion.minY + 30f * itemScale, 0.15f * boxRegion.height,
		numberFont, highlightTextColor, 0, 0f,
		rgb(0, 0, 0), 0.02f * boxRegion.height,
		0.02f * boxRegion.height, TextAlignment.LEFT,
	)
	textBatch.drawString(
		"?", boxRegion.maxX - 5f * itemScale, boxRegion.minY + 29f * itemScale,
		0.1f * boxRegion.height, simpleFont, weakTextColor, TextAlignment.RIGHT,
	)

	textBatch.drawString(
		"E", region.boundX - 0.25f * region.height, region.boundY - 0.08f * region.height,
		0.02f * region.height, simpleFont, highlightTextColor, TextAlignment.RIGHT,
	)
	textBatch.drawString(
		"to confirm", region.boundX - 0.24f * region.height, region.boundY - 0.08f * region.height,
		0.02f * region.height, simpleFont, weakTextColor,
	)
	textBatch.drawString(
		"Q", region.boundX - 0.247f * region.height, region.boundY - 0.04f * region.height,
		0.02f * region.height, simpleFont, highlightTextColor, TextAlignment.RIGHT,
	)
	textBatch.drawString(
		"to cancel", region.boundX - 0.24f * region.height, region.boundY - 0.04f * region.height,
		0.02f * region.height, simpleFont, weakTextColor,
	)
}

