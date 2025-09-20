package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.state.util.Rectangle
import kotlin.math.max

private val SECTIONS = arrayOf("Party", "Skills", "Inventory", "Map", "Video  Settings")

internal fun renderInGameMenuSectionList(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {

		val baseLineColor = srgbToLinear(rgb(208, 193, 142))
		val selectedLineColor = srgbToLinear(rgb(165, 205, 254))
		val baseTintColor = srgbToLinear(rgba(111, 92, 53, 183))
		val selectedTintColor = srgbToLinear(rgba(6, 81, 156, 182))

		val font = context.bundle.getFont(context.content.fonts.large2.index)
		val lowBaseTextColor = srgbToLinear(rgb(214, 170, 98))
		val lowSelectedTextColor = srgbToLinear(rgb(104, 179, 252))
		val highBaseTextColor = srgbToLinear(rgb(249, 237, 210))
		val highSelectedTextColor = srgbToLinear(rgb(230, 255, 255))
		val shadowColor = rgba(0, 0, 0, 100)

		for ((index, section) in SECTIONS.withIndex()) {
			val lineY = region.minY + (index + 1) * region.height / 13
			val lineWidth = max(1, region.height / 500)

			val lineColor: Int
			val tintColor: Int
			val lowTextColor: Int
			val highTextColor: Int
			if (menu.currentTab.getText() == section) {
				lineColor = selectedLineColor
				tintColor = selectedTintColor
				lowTextColor = lowSelectedTextColor
				highTextColor = highSelectedTextColor
			} else {
				lineColor = baseLineColor
				tintColor = baseTintColor
				lowTextColor = lowBaseTextColor
				highTextColor = highBaseTextColor
			}

			colorBatch.fill(region.minX, lineY, region.maxX, lineY + lineWidth - 1, lineColor)
			colorBatch.gradient(
				region.minX, lineY - region.height / 30, region.maxX, lineY - 1,
				0, tintColor, 0
			)
			textBatch.drawFancyString(
				section, region.maxX - region.height / 100f, lineY - region.height / 100f,
				region.height / 25f, font, lowTextColor,
				shadowColor, region.height / 250f, TextAlignment.RIGHT,
				lowTextColor, highTextColor, highTextColor, highTextColor,
				0.5f, 0.5f, 0.5f, 0.5f,
			)
		}
	}
}
