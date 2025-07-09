package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.state.ingame.menu.VideoSettingsTab
import mardek.state.util.Rectangle
import org.lwjgl.vulkan.VkPhysicalDeviceProperties

internal fun renderVideoSettingsTab(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		val tab = menu.currentTab as VideoSettingsTab
		val settings = context.videoSettings
		tab.settings = settings

		val font = context.bundle.getFont(context.content.fonts.basic1.index)
		val baseTextColor = srgbToLinear(rgb(238, 203, 127))
		val selectedTextColor = srgbToLinear(rgb(240, 224, 185))
		val warningColor = rgb(200, 50, 50)

		fun textColor(property: Int, enabled: Boolean): Int {
			val color = if (tab.selectedProperty == property) selectedTextColor else baseTextColor
			if (!enabled) return changeAlpha(color, 20)
			return color
		}

		val textHeight = 0.04f * region.height
		val baseX = region.minX + region.width * 0.1f
		val device = settings.availableDevices[settings.preferredDevice] as VkPhysicalDeviceProperties
		textBatch.drawString(
			"*", baseX - 0.025f * region.height, region.minY + 0.21f * region.height,
			textHeight, font, warningColor, TextAlignment.RIGHT,
		)
		textBatch.drawString(
			"Graphics card ${settings.preferredDevice}: ${device.deviceNameString()}", baseX,
			region.minY + 0.2f * region.height, textHeight, font,
			textColor(0, settings.availableDevices.size > 1),
		)
		textBatch.drawString(
			"Cap FPS", baseX, region.minY + 0.3f * region.height,
			textHeight, font, textColor(1, settings.canUncapFps),
		)
		val toggle1 = if (settings.capFps) context.content.ui.skillToggled else context.content.ui.skillNotToggled
		imageBatch.coloredScale(
			baseX + region.height * 0.25f, region.minY + 0.26f * region.height,
			0.04f * region.height / toggle1.height, toggle1.index, 0,
			rgba(1f, 1f, 1f, if (settings.canUncapFps) 1f else 0.05f)
		)

		textBatch.drawString(
			"Show FPS", baseX, region.minY + 0.4f * region.height,
			textHeight, font, textColor(2, true),
		)
		val toggle2 = if (settings.showFps) context.content.ui.skillToggled else context.content.ui.skillNotToggled
		imageBatch.simpleScale(
			baseX + region.height * 0.25f, region.minY + 0.36f * region.height,
			0.04f * region.height / toggle2.height, toggle2.index,
		)

		textBatch.drawString(
			"*", region.minX + 0.05f * region.height, region.maxY - 0.1f * region.height,
			textHeight, font, warningColor, TextAlignment.RIGHT,
		)
		textBatch.drawString(
			"Requires restart", region.minX + 0.075f * region.height,
			region.maxY - 0.11f * region.height, textHeight, font, baseTextColor,
		)
	}
}
