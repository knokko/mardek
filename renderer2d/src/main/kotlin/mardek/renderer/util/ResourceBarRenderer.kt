package mardek.renderer.util

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.RenderContext
import mardek.state.util.Rectangle

class ResourceBarRenderer(
	private val context: RenderContext,
	private val resourceType: ResourceType,
	val barRegion: Rectangle,
	private val colorBatch: Vk2dColorBatch,
	private val textBatch: Vk2dGlyphBatch,
) {

	fun renderBar(currentValue: Int, maxValue: Int, opacity: Float = 1f) {
		val backgroundColor = srgbToLinear(rgb(58, 43, 31))
		val remainingWidth = currentValue * (barRegion.width - 1) / maxValue
		val entry = resourceType.chooseColor(currentValue, maxValue)
		colorBatch.fill(
			barRegion.minX + remainingWidth, barRegion.minY,
			barRegion.maxX, barRegion.maxY,
			changeAlpha(backgroundColor, opacity),
		)

		colorBatch.gradient(
			barRegion.minX, barRegion.minY, barRegion.minX + remainingWidth, barRegion.maxY,
			changeAlpha(entry.bottomLeftColor, opacity),
			changeAlpha(entry.bottomRightColor, opacity),
			changeAlpha(entry.bottomLeftColor, opacity),
		)
		if (entry.topGradient) {
			colorBatch.gradient(
				barRegion.minX, barRegion.minY + barRegion.height / 7,
				barRegion.minX + remainingWidth, barRegion.minY + 4 * barRegion.height / 7,
				changeAlpha(entry.topLeftColor, 0.4f * opacity),
				changeAlpha(entry.topRightColor, 0.4f * opacity),
				changeAlpha(entry.topLeftColor, opacity),
			)
		} else {
			colorBatch.gradient(
				barRegion.minX, barRegion.minY + barRegion.height / 7,
				barRegion.minX + remainingWidth, barRegion.minY + 4 * barRegion.height / 7,
				changeAlpha(entry.topLeftColor, opacity),
				changeAlpha(entry.topRightColor, opacity),
				changeAlpha(entry.topLeftColor, opacity),
			)
		}
	}

	fun renderLost(currentValue: Int, oldValue: Int, maxValue: Int, opacity: Float) {
		if (oldValue > currentValue) {
			colorBatch.fill(
				barRegion.minX + currentValue * (barRegion.width - 1) / maxValue, barRegion.minY,
				barRegion.minX + oldValue * (barRegion.width - 1) / maxValue,
				barRegion.maxY, rgba(1f, 0f, 0f, opacity)
			)
		}
	}

	fun renderTextBelowBar(currentValue: Int, maxValue: Int) {
		val entry = resourceType.chooseColor(currentValue, maxValue)
		val font = context.bundle.getFont(context.content.fonts.large1.index)
		val shadowOffset = 0.2f * barRegion.height
		textBatch.drawShadowedString(
			"$currentValue/$maxValue", (barRegion.minX + barRegion.maxX) * 0.5f,
			barRegion.maxY + 1.8f * barRegion.height, 1.5f * barRegion.height,
			font, entry.textColor, 0, 0f, entry.shadowColor,
			shadowOffset, shadowOffset, TextAlignment.CENTERED
		)
	}

	fun renderTextOverBar(currentValue: Int, maxValue: Int) {
		val entry = resourceType.chooseColor(currentValue, maxValue)
		val splitX = barRegion.minX + barRegion.width * 5f / 9f
		val marginX = barRegion.width / 30f
		val font = context.bundle.getFont(context.content.fonts.large1.index)
		val shadowOffset = 0.125f * barRegion.height
		textBatch.drawShadowedString(
			currentValue.toString(), splitX - marginX, barRegion.maxY + barRegion.height * 0.33f,
			1.75f * barRegion.height, font, entry.textColor, 0, 0f,
			entry.shadowColor, shadowOffset, shadowOffset, TextAlignment.RIGHT
		)
		textBatch.drawShadowedString(
			maxValue.toString(), splitX + marginX, barRegion.maxY + barRegion.height * 0.2f,
			1.33f * barRegion.height, font, entry.textColor, 0, 0f,
			entry.shadowColor, shadowOffset, shadowOffset, TextAlignment.LEFT
		)
	}

	fun renderCurrentOverBar(currentValue: Int, maxValue: Int, opacity: Float = 1f) {
		val entry = resourceType.chooseColor(currentValue, maxValue)
		val font = context.bundle.getFont(context.content.fonts.large1.index)
		val shadowOffset = 0.125f * barRegion.height
		textBatch.drawShadowedString(
			currentValue.toString(), barRegion.maxX - 0.1f * barRegion.width,
			barRegion.maxY + barRegion.height * 0.5f, barRegion.height * 1.75f, font,
			changeAlpha(entry.textColor, opacity), 0, 0f,
			changeAlpha(entry.shadowColor, opacity),
			shadowOffset, shadowOffset, TextAlignment.RIGHT,
		)
	}

	fun renderOpeningBracket() {
		val color = srgbToLinear(rgb(208, 193, 142))
		val width = barRegion.height / 8
		val gap1 = barRegion.height / 7
		val gap2 = barRegion.height / 4
		val x1 = barRegion.minX + width + barRegion.height / 3
		val x2 = barRegion.minX + barRegion.height / 3
		colorBatch.fill(
			barRegion.minX, barRegion.minY - gap1, barRegion.minX + width - 1,
			barRegion.maxY + gap1, color
		)
		colorBatch.fillUnaligned(
			x1, barRegion.minY - gap2, barRegion.minX + width, barRegion.minY,
			barRegion.minX, barRegion.minY - gap1, x2, barRegion.minY - gap2, color
		)
		colorBatch.fillUnaligned(
			x1, barRegion.boundY + gap2, barRegion.minX + width, barRegion.boundY,
			barRegion.minX, barRegion.boundY + gap1, x2, barRegion.boundY + gap2, color
		)
	}

	fun renderClosingBracket() {
		val color = srgbToLinear(rgb(208, 193, 142))
		val width = barRegion.height / 8
		val gap1 = barRegion.height / 7
		val gap2 = barRegion.height / 4
		val x1 = barRegion.boundX - width - barRegion.height / 3
		val x2 = barRegion.boundX - barRegion.height / 3
		colorBatch.fill(
			barRegion.boundX - width, barRegion.minY - gap1, barRegion.maxX,
			barRegion.maxY + gap1, color
		)
		colorBatch.fillUnaligned(
			x1, barRegion.minY - gap2, barRegion.boundX - width, barRegion.minY,
			barRegion.boundX, barRegion.minY - gap1, x2, barRegion.minY - gap2, color
		)
		colorBatch.fillUnaligned(
			x1, barRegion.boundY + gap2, barRegion.boundX - width, barRegion.boundY,
			barRegion.boundX, barRegion.boundY + gap1, x2, barRegion.boundY + gap2, color
		)
	}
}

class ResourceColorEntry(
	val belowThreshold: Float, val bottomLeftColor: Int, val bottomRightColor: Int,
	val topGradient: Boolean, val topLeftColor: Int, val topRightColor: Int,
	val textColor: Int, val shadowColor: Int,
)

enum class ResourceType(private val entries: List<ResourceColorEntry>) {
	Health(listOf(
		ResourceColorEntry(
			belowThreshold = 1f,
			bottomLeftColor = srgbToLinear(rgb(70, 133, 25)),
			bottomRightColor = srgbToLinear(rgb(44, 76, 31)),
			topGradient = true,
			topLeftColor = srgbToLinear(rgb(151, 191, 122)),
			topRightColor = srgbToLinear(rgb(137, 162, 125)),
			textColor = srgbToLinear(rgb(127, 231, 57)),
			shadowColor = srgbToLinear(rgb(0, 51, 0)),
		),
		ResourceColorEntry(
			belowThreshold = 0.4f,
			bottomLeftColor = srgbToLinear(rgb(134, 97, 32)),
			bottomRightColor = srgbToLinear(rgb(124, 88, 31)),
			topGradient = true,
			topLeftColor = srgbToLinear(rgb(172, 153, 100)),
			topRightColor = srgbToLinear(rgb(164, 147, 100)),
			textColor = srgbToLinear(rgb(207, 230, 57)),
			shadowColor = srgbToLinear(rgb(0, 51, 0)),
		),
		ResourceColorEntry(
			belowThreshold = 0.2f,
			bottomLeftColor = srgbToLinear(rgb(170, 31, 21)),
			bottomRightColor = srgbToLinear(rgb(155, 31, 21)),
			topGradient = true,
			topLeftColor = srgbToLinear(rgb(195, 107, 90)),
			topRightColor = srgbToLinear(rgb(180, 92, 97)),
			textColor = srgbToLinear(rgb(255, 102, 102)),
			shadowColor = srgbToLinear(rgb(0, 51, 0)),
		)
	)),
	Mana(listOf(
		ResourceColorEntry(
			belowThreshold = 1f,
			bottomLeftColor = srgbToLinear(rgb(40, 109, 129)),
			bottomRightColor = srgbToLinear(rgb(45, 170, 115)),
			topGradient = true,
			topLeftColor = srgbToLinear(rgb(127, 171, 187)),
			topRightColor = srgbToLinear(rgb(130, 204, 181)),
			textColor = srgbToLinear(rgb(35, 247, 255)),
			shadowColor = srgbToLinear(rgb(70, 28, 128)),
		)
	)),
	Experience(listOf(
		ResourceColorEntry(
			belowThreshold = 1f,
			bottomLeftColor = srgbToLinear(rgb(186, 152, 76)),
			bottomRightColor = srgbToLinear(rgb(154, 111, 38)),
			topGradient = true,
			topLeftColor = srgbToLinear(rgb(219, 199, 147)),
			topRightColor = srgbToLinear(rgb(187, 161, 112)),
			textColor = srgbToLinear(rgb(250, 217, 93)),
			shadowColor = srgbToLinear(rgb(90, 52, 22)),
		)
	)),
	SkillEnable(listOf(
		ResourceColorEntry(
			belowThreshold = 1f,
			bottomLeftColor = srgbToLinear(rgb(17, 154, 35)),
			bottomRightColor = srgbToLinear(rgb(19, 107, 27)),
			topGradient = true,
			topLeftColor = srgbToLinear(rgb(170, 223, 167)),
			topRightColor = srgbToLinear(rgb(170, 209, 165)),
			textColor = srgbToLinear(rgb(19, 241, 70)),
			shadowColor = srgbToLinear(rgb(68, 98, 55)),
		)
	)),
	SkillMastery(listOf(
		ResourceColorEntry(
			belowThreshold = 1f,
			bottomLeftColor = srgbToLinear(rgb(173, 46, 32)),
			bottomRightColor = srgbToLinear(rgb(150, 35, 27)),
			topGradient = false,
			topLeftColor = srgbToLinear(rgb(174, 84, 70)),
			topRightColor = srgbToLinear(rgb(140, 55, 45)),
			textColor = srgbToLinear(rgb(254, 94, 94)),
			shadowColor = srgbToLinear(rgb(126, 1, 1)),
		)
	));

	fun chooseColor(currentValue: Int, maxValue: Int): ResourceColorEntry {
		var entry = entries[0]
		val fraction = currentValue.toFloat() / maxValue.toFloat()
		for (candidate in entries) {
			if (fraction > candidate.belowThreshold) break
			entry = candidate
		}
		return entry
	}
}
