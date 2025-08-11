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

	fun renderBar(currentValue: Int, maxValue: Int) {
		val backgroundColor = srgbToLinear(rgb(58, 43, 31))
		val remainingWidth = currentValue * barRegion.width / maxValue
		val entry = resourceType.chooseColor(currentValue, maxValue)
		colorBatch.fill(
			barRegion.minX + remainingWidth, barRegion.minY,
			barRegion.maxX, barRegion.maxY, backgroundColor
		)
		colorBatch.gradient(
			barRegion.minX, barRegion.minY, barRegion.minX + remainingWidth, barRegion.maxY,
			entry.bottomLeftColor, entry.bottomRightColor, entry.bottomLeftColor
		)
		if (entry.topGradient) {
			colorBatch.gradient(
				barRegion.minX, barRegion.minY + barRegion.height / 7,
				barRegion.minX + remainingWidth, barRegion.minY + 4 * barRegion.height / 7,
				changeAlpha(entry.topLeftColor, 100),
				changeAlpha(entry.topRightColor, 100), entry.topLeftColor
			)
		} else {
			colorBatch.gradient(
				barRegion.minX, barRegion.minY + barRegion.height / 7,
				barRegion.minX + remainingWidth, barRegion.minY + 4 * barRegion.height / 7,
				entry.topLeftColor, entry.topRightColor, entry.topLeftColor
			)
		}
	}

	fun renderLost(currentValue: Int, oldValue: Int, maxValue: Int, opacity: Float) {
		if (oldValue > currentValue) {
			colorBatch.fill(
				barRegion.minX + currentValue * barRegion.width / maxValue, barRegion.minY,
				barRegion.minX + oldValue * barRegion.width / maxValue,
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
		val splitX = barRegion.minX + barRegion.width * 5 / 9
		val marginX = barRegion.width / 30
		val font = context.bundle.getFont(context.content.fonts.large1.index)
		textBatch.drawString(
			currentValue.toString(), splitX - marginX, barRegion.maxY + barRegion.height / 3,
			7 * barRegion.height / 4, font, entry.textColor, TextAlignment.RIGHT
		)
		// TODO Shadows?
//		context.uiRenderer.drawString(
//			context.resources.font, currentValue.toString(), entry.textColor, IntArray(0),
//			barRegion.minX, barRegion.minY - 2 * barRegion.height,
//			splitX - marginX, barRegion.maxY + barRegion.height,
//			barRegion.maxY + barRegion.height / 3, 7 * barRegion.height / 4, 1, TextAlignment.RIGHT
//		)
		textBatch.drawString(
			maxValue.toString(), splitX + marginX, barRegion.maxY + barRegion.height / 5,
			4 * barRegion.height / 3, font, entry.textColor
		)
//		context.uiRenderer.drawString(
//			context.resources.font, maxValue.toString(), entry.textColor, IntArray(0),
//			splitX + marginX, barRegion.minY - barRegion.height,
//			barRegion.maxX, barRegion.maxY + barRegion.height / 2,
//			barRegion.maxY + barRegion.height / 5, 4 * barRegion.height / 3, 1, TextAlignment.LEFT
//		)
	}
//
//	fun renderCurrentOverBar(currentValue: Int, maxValue: Int) {
//		val entry = resourceType.chooseColor(currentValue, maxValue)
//		context.uiRenderer.drawString(
//			context.resources.font, currentValue.toString(), entry.textColor, IntArray(0),
//			barRegion.minX, barRegion.minY - barRegion.height,
//			barRegion.maxX - barRegion.width / 10, barRegion.maxY + barRegion.height / 2,
//			barRegion.maxY + barRegion.height / 3, 3 * barRegion.height / 2, 1, TextAlignment.RIGHT
//		)
//	}
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
