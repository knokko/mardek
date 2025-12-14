package mardek.renderer.menu

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.util.renderDescription
import mardek.state.ingame.menu.QuestsTab
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

internal fun renderQuestsTab(menuContext: MenuRenderContext, region: Rectangle) {
	fun chooseColor(selected: Boolean) = if (selected) {
		srgbToLinear(rgb(164, 204, 253))
	} else {
		srgbToLinear(rgb(238, 203, 127))
	}

	menuContext.apply {
		val pointerSprite = context.content.ui.pointer
		val questSprite = context.content.ui.questIcon

		val state = menu.currentTab as QuestsTab
		val separatorY = region.minY + region.height / 13
		val leftBarColor = srgbToLinear(rgb(93, 75, 43))
		colorBatch.gradient(
			region.minX, separatorY, region.minX + region.height / 2, region.maxY,
			leftBarColor, 0, leftBarColor
		)

		val lineColor = srgbToLinear(rgb(208, 193, 142))
		colorBatch.fill(region.minX, separatorY - region.height / 500, region.maxX, separatorY, lineColor)

		fun drawTopButton(text: String, minX: Int, isSelected: Boolean) {
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
			textBatch.drawString(
				text, 0.5f * (minX + maxX), maxY - 0.006f * region.height,
				region.height * 0.03f, font, textColor, TextAlignment.CENTERED,
			)

			if (isSelected && !state.inside) {
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

		drawTopButton("active", region.minX + region.height / 35, !state.showCompleted)
		drawTopButton("complete", region.minX + 2 * region.height / 9, state.showCompleted)

		val quests = if (state.showCompleted) state.quests.completed else state.quests.active
		val shadowColor = srgbToLinear(rgb(90, 52, 22))
		for ((index, quest) in quests.withIndex()) {
			val isSelected = state.inside && state.questIndex == index
			run {
				val font = context.bundle.getFont(context.content.fonts.large1.index)
				val baseY = separatorY + (index + 1) * 0.07f * region.height
				textBatch.drawString(
					quest.tabName, region.minX + 0.095f * region.height, baseY,
					region.height * 0.0225f, font, chooseColor(isSelected),
				)
				imageBatch.simpleScale(
					region.minX + 0.045f * region.height, baseY - 0.035f * region.height,
					0.045f * region.height / questSprite.height, questSprite.index,
				)
				if (isSelected) {
					imageBatch.simpleScale(
						region.minX - 0.0075f * determinePointerOffset() * region.height,
						baseY - 0.025f * region.height,
						0.0325f * region.height / pointerSprite.height, pointerSprite.index,
					)
				}
			}

			if (isSelected) {
				val font = context.bundle.getFont(context.content.fonts.basic2.index)
				val minX = region.minX + 0.45f * region.height
				val shadowOffset = 0.004f * region.height
				textBatch.drawShadowedString(
					quest.title, minX, separatorY - 0.015f * region.height,
					region.height * 0.025f, font,
					srgbToLinear(rgb(255, 255, 152)),
					0, 0f, shadowColor,
					shadowOffset, shadowOffset, TextAlignment.LEFT,
				)

				var lineY = separatorY + 0.045f * region.height
				val spaceX = region.maxX - minX.roundToInt()
				if (spaceX > 50) {
					renderDescription(quest.description, spaceX / (region.height / 70)) { line ->
						textBatch.drawShadowedString(
							line, minX, lineY, 0.02f * region.height, font,
							srgbToLinear(rgb(238, 203, 127)),
							0, 0f, shadowColor,
							shadowOffset, shadowOffset, TextAlignment.LEFT
						)
						lineY += 0.0375f * region.height
					}
				}
			}
		}
		// TODO CHAP1 Create chat log
		// TODO CHAP1 Create area transition black fade
	}
}
