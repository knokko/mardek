package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.state.util.Rectangle
import kotlin.math.max

private val SECTIONS = arrayOf("Party", "Skills", "Inventory", "Map", "Quests", "Encyclopaedia", "Video  Settings")

internal fun renderInGameMenuSectionList(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {

		val baseLineColor = srgbToLinear(rgb(208, 193, 142))
		val selectedLineColor = srgbToLinear(rgb(165, 205, 254))
		val baseTintColor = srgbToLinear(rgba(111, 92, 53, 183))
		val selectedTintColor = srgbToLinear(rgba(6, 81, 156, 182))

		val font = context.bundle.getFont(context.content.fonts.large2.index)

		for ((index, section) in SECTIONS.withIndex()) {
			val lineY = region.minY + (index + 1) * region.height / 13
			val lineWidth = max(1, region.height / 500)

			val lineColor: Int
			val tintColor: Int
			val selected = menu.currentTab.getText() == section
			if (selected) {
				lineColor = selectedLineColor
				tintColor = selectedTintColor
			} else {
				lineColor = baseLineColor
				tintColor = baseTintColor
			}

			colorBatch.fill(region.minX, lineY, region.maxX, lineY + lineWidth - 1, lineColor)
			colorBatch.gradient(
				region.minX, lineY - region.height / 30, region.maxX, lineY - 1,
				0, tintColor, 0
			)
			fancyTextBatch.drawString(
				section, region.maxX - region.height / 100f, lineY - region.height / 100f,
				0f, region.height / 25f, font,
				MardekTextStyles.menuSection(selected), TextAlignment.RIGHT,
			)
		}
	}
}
