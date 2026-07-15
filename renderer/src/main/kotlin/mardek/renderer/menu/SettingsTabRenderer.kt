package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.state.ingame.menu.SettingsTab
import mardek.state.util.Rectangle
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import kotlin.math.max
import kotlin.math.min

internal fun renderSettingsTab(menuContext: MenuRenderContext, region: Rectangle) {
	menuContext.run {
		val tab = menu.currentTab as SettingsTab
		tab.settings = context.userSettings

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

		val lineColor = srgbToLinear(rgb(208, 193, 142))
		val separatorY = region.minY + region.height / 13
		colorBatch.fill(region.minX, separatorY - region.height / 500, region.maxX, separatorY, lineColor)

		val pointerSprite = context.content.ui.pointer

		fun drawTopButton(text: String, minX: Int, isSelected: Boolean) {
			fun chooseColor(selected: Boolean) = if (selected) {
				srgbToLinear(rgb(164, 204, 253))
			} else {
				srgbToLinear(rgb(238, 203, 127))
			}

			val backgroundColor = if (isSelected) {
				srgbToLinear(rgb(20, 65, 114))
			} else {
				srgbToLinear(rgb(91, 73, 42))
			}

			val minY = region.minY + region.height / 125
			val maxX = minX + region.height / 6
			val maxY = region.minY + region.height / 16
			colorBatch.gradient(
				minX, minY, maxX, maxY,
				backgroundColor, backgroundColor, 0
			)
			colorBatch.fill(
				minX, maxY, maxX, maxY + region.height / 600,
				if (isSelected) srgbToLinear(rgb(165, 205, 254)) else lineColor
			)
			val textColor = chooseColor(isSelected)

			val font = context.bundle.getFont(context.content.fonts.large2.index)
			simpleTextBatch.drawString(
				text, 0.5f * (minX + maxX), maxY - 0.006f * region.height,
				region.height * 0.03f, font, textColor, TextAlignment.CENTERED,
			)

			if (isSelected && !tab.inside) {
				val scale = 0.03f * region.height / pointerSprite.height
				context.addImageBatch(2).rotated(
					0.5f * (minX + maxX),
					region.minY - (0.0075f - 0.01f * determinePointerOffset()) * region.height,
					270f,
					scale, pointerSprite.index,
					0, -1,
				)
			}
		}

		drawTopButton("Video", region.minX + region.height / 35, tab.selectedType == 0)
		drawTopButton("Audio", region.minX + 2 * region.height / 9, tab.selectedType == 1)

		if (tab.selectedType == 0) {
			val settings = context.userSettings.videoSettings
			val device = settings.availableDevices[settings.preferredDevice] as VkPhysicalDeviceProperties
			simpleTextBatch.drawString(
				"*", baseX - 0.025f * region.height, region.minY + 0.21f * region.height,
				textHeight, font, warningColor, TextAlignment.RIGHT,
			)
			simpleTextBatch.drawString(
				"Graphics card ${settings.preferredDevice}: ${device.deviceNameString()}", baseX,
				region.minY + 0.2f * region.height, textHeight, font,
				textColor(0, settings.availableDevices.size > 1),
			)

			simpleTextBatch.drawString(
				"Cap FPS", baseX, region.minY + 0.3f * region.height,
				textHeight, font, textColor(1, settings.canUncapFps),
			)
			val toggle1 = if (settings.capFps) context.content.ui.skillToggled else context.content.ui.skillNotToggled
			imageBatch.coloredScale(
				baseX + region.height * 0.25f, region.minY + 0.26f * region.height,
				0.04f * region.height / toggle1.height, toggle1.index, 0,
				rgba(1f, 1f, 1f, if (settings.canUncapFps) 1f else 0.05f)
			)

			simpleTextBatch.drawString(
				"Show FPS", baseX, region.minY + 0.4f * region.height,
				textHeight, font, textColor(2, true),
			)
			val toggle2 = if (settings.showFps) context.content.ui.skillToggled else context.content.ui.skillNotToggled
			imageBatch.simpleScale(
				baseX + region.height * 0.25f, region.minY + 0.36f * region.height,
				0.04f * region.height / toggle2.height, toggle2.index,
			)

			simpleTextBatch.drawString(
				"*", baseX - 0.025f * region.height, region.minY + 0.51f * region.height,
				textHeight, font, warningColor, TextAlignment.RIGHT,
			)
			simpleTextBatch.drawString(
				"Frames In Flight: ${settings.framesInFlight}", baseX,
				region.minY + 0.5f * region.height, textHeight, font,
				textColor(3, true),
			)

			simpleTextBatch.drawString(
				"Delay rendering", baseX, region.minY + 0.6f * region.height,
				textHeight, font, textColor(4, true),
			)
			val toggle3 = if (settings.delayRendering) context.content.ui.skillToggled else context.content.ui.skillNotToggled
			imageBatch.simpleScale(
				baseX + region.height * 0.4f, region.minY + 0.56f * region.height,
				0.04f * region.height / toggle3.height, toggle3.index,
			)

			simpleTextBatch.drawString(
				"*", region.minX + 0.05f * region.height, region.maxY - 0.1f * region.height,
				textHeight, font, warningColor, TextAlignment.RIGHT,
			)
			simpleTextBatch.drawString(
				"Requires restart", region.minX + 0.075f * region.height,
				region.maxY - 0.11f * region.height, textHeight, font, baseTextColor,
			)
		}

		if (tab.selectedType == 1) {
			val settings = context.userSettings.audioSettings

			fun renderVolumeSetting(name: String, baseY: Int, index: Int, currentVolume: Int) {
				simpleTextBatch.drawString(
					"$name volume:", baseX, baseY.toFloat(), textHeight, font,
					textColor(index, true),
				)

				val sliderMinX = baseX.toInt() + 2 * region.height / 5
				val sliderMinY = baseY - region.height / 40
				val sliderMaxX = region.maxX - 22 * region.height / 100
				val sliderMaxY = baseY - region.height / 90
				val sliderLineWidth = max(1, region.height / 200)
				colorBatch.fill(
					sliderMinX, sliderMinY, sliderMaxX, sliderMaxY,
					srgbToLinear(rgb(60, 40, 0)),
				)
				colorBatch.fill(
					sliderMinX + sliderLineWidth, sliderMinY + sliderLineWidth,
					sliderMaxX - sliderLineWidth, sliderMaxY - sliderLineWidth,
					srgbToLinear(rgb(110, 80, 5)),
				)

				val cursorX = sliderMinX + currentVolume * (sliderMaxX - sliderMinX) / 100
				val cursorLineWidth = min(1, region.height / 300)
				colorBatch.fill(
					cursorX - region.height / 200, sliderMinY - region.height / 80,
					cursorX + region.height / 200, sliderMaxY + region.height / 80,
					srgbToLinear(rgb(50, 50, 50))
				)
				colorBatch.fill(
					cursorX - region.height / 200 + cursorLineWidth,
					sliderMinY - region.height / 80 + cursorLineWidth,
					cursorX + region.height / 200 - cursorLineWidth,
					sliderMaxY + region.height / 80 - cursorLineWidth,
					srgbToLinear(rgb(100, 100, 100))
				)

				simpleTextBatch.drawString(
					"$currentVolume %", region.maxX - region.height * 0.05f,
					baseY.toFloat(), textHeight, font,
					textColor(index, true), TextAlignment.RIGHT,
				)
			}

			renderVolumeSetting(
				"Master", region.minY + region.height / 5,
				0, settings.masterVolume,
			)
			renderVolumeSetting(
				"Music", region.minY + 3 * region.height / 10,
				1, settings.musicVolume,
			)
			renderVolumeSetting(
				"Sounds", region.minY + 4 * region.height / 10,
				2, settings.soundEffectVolume,
			)
		}
	}
}
