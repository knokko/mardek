package mardek.renderer.ui

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.InGameRenderContext
import mardek.state.title.AbsoluteRectangle

private fun changeAlpha(color: Int, alpha: Int) = rgba(red(color), green(color), blue(color), alpha.toByte())

class ResourceBarRenderer(
	private val context: InGameRenderContext,
	private val resourceType: ResourceType,
	private val barRegion: AbsoluteRectangle
) {

	fun renderBar(currentValue: Int, maxValue: Int) {
		val backgroundColor = srgbToLinear(rgb(58, 43, 31))
		val remainingWidth = currentValue * barRegion.width / maxValue
		val entry = resourceType.chooseColor(currentValue, maxValue)
		context.uiRenderer.fillColor(
			barRegion.minX, barRegion.minY, barRegion.maxX, barRegion.maxY, backgroundColor,
			Gradient(
				0, 0, remainingWidth, barRegion.height,
				entry.bottomLeftColor, entry.bottomRightColor, entry.bottomLeftColor
			),
			if (entry.topGradient) Gradient(
				0, barRegion.height / 7, remainingWidth, 3 * barRegion.height / 7,
				changeAlpha(entry.topLeftColor, 100), changeAlpha(entry.topRightColor, 100), entry.topLeftColor
			) else Gradient(
				0, barRegion.height / 7, remainingWidth, 3 * barRegion.height / 7,
				entry.topLeftColor, entry.topRightColor, entry.topLeftColor
			)
		)
	}

	fun renderTextBelowBar(currentValue: Int, maxValue: Int) {
		val entry = resourceType.chooseColor(currentValue, maxValue)
		context.uiRenderer.drawString(
			context.resources.font, "$currentValue/$maxValue", entry.textColor, IntArray(0),
			barRegion.minX, barRegion.minY, barRegion.maxX, barRegion.maxY + 2 * barRegion.height,
			barRegion.maxY + 9 * barRegion.height / 5, 3 * barRegion.height / 2, 1, TextAlignment.CENTER
		)
	}

	fun renderTextOverBar(currentValue: Int, maxValue: Int) {
		val entry = resourceType.chooseColor(currentValue, maxValue)
		val splitX = barRegion.minX + barRegion.width * 5 / 9
		val marginX = barRegion.width / 30
		context.uiRenderer.drawString(
			context.resources.font, currentValue.toString(), entry.textColor, IntArray(0),
			barRegion.minX, barRegion.minY - 2 * barRegion.height,
			splitX - marginX, barRegion.maxY + barRegion.height,
			barRegion.maxY + barRegion.height / 3, 7 * barRegion.height / 4, 1, TextAlignment.RIGHT
		)
		context.uiRenderer.drawString(
			context.resources.font, maxValue.toString(), entry.textColor, IntArray(0),
			splitX + marginX, barRegion.minY - barRegion.height,
			barRegion.maxX, barRegion.maxY + barRegion.height / 2,
			barRegion.maxY + barRegion.height / 5, 4 * barRegion.height / 3, 1, TextAlignment.LEFT
		)
	}

	fun renderCurrentOverBar(currentValue: Int, maxValue: Int) {
		val entry = resourceType.chooseColor(currentValue, maxValue)
		context.uiRenderer.drawString(
			context.resources.font, currentValue.toString(), entry.textColor, IntArray(0),
			barRegion.minX, barRegion.minY - barRegion.height,
			barRegion.maxX - barRegion.width / 10, barRegion.maxY + barRegion.height / 2,
			barRegion.maxY + barRegion.height / 3, 3 * barRegion.height / 2, 1, TextAlignment.RIGHT
		)
	}
}

class ResourceColorEntry(
	val belowThreshold: Float, val bottomLeftColor: Int, val bottomRightColor: Int,
	val topGradient: Boolean, val topLeftColor: Int, val topRightColor: Int, val textColor: Int
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
			textColor = srgbToLinear(rgb(127, 231, 57))
		),
		ResourceColorEntry(
			belowThreshold = 0.4f,
			bottomLeftColor = srgbToLinear(rgb(134, 97, 32)),
			bottomRightColor = srgbToLinear(rgb(124, 88, 31)),
			topGradient = true,
			topLeftColor = srgbToLinear(rgb(172, 153, 100)),
			topRightColor = srgbToLinear(rgb(164, 147, 100)),
			textColor = srgbToLinear(rgb(207, 230, 57))
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
			textColor = srgbToLinear(rgb(35, 247, 255))
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
			textColor = srgbToLinear(rgb(250, 217, 93))
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
			textColor = srgbToLinear(rgb(19, 241, 70))
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
			textColor = srgbToLinear(rgb(254, 94, 94))
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
