package mardek.renderer.ui

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.assets.GameAssets
import mardek.state.ingame.InGameState

class InGameMenuRenderer(
	private val assets: GameAssets,
	private val sharedUi: SharedUiResources,
	private val state: InGameState,
) {

	fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height)

		val renderer = sharedUi.uiRenderers[frameIndex]
		renderer.begin(recorder, targetImage)

		val transform = CoordinateTransform.create(SpaceLayout.Simple, targetImage.width, targetImage.height)

		val barColor = srgbToLinear(rgb(24, 14, 10))
		val upperBar = transform.transform(0f, 0.92f, 1f, 1f)
		val lowerBar = transform.transform(0f, 0f, 1f, 0.08f)

		renderer.fillColor(upperBar.minX, upperBar.minY, upperBar.maxX, upperBar.maxY, barColor)
		renderer.fillColor(lowerBar.minX, lowerBar.minY, lowerBar.maxX, lowerBar.maxY, barColor)

		val leftColor = srgbToLinear(rgba(54, 37, 21, 179))
		val rightColor = srgbToLinear(rgba(132, 84, 53, 179))
		renderer.fillColor(
			lowerBar.minX, upperBar.boundY, lowerBar.maxX, lowerBar.minY - 1, barColor,
			Gradient(
				0, 0, lowerBar.width, lowerBar.minY - upperBar.boundY,
				leftColor, rightColor, leftColor
			)
		)

		val area = state.progress.currentArea
		if (area != null) {
			renderer.drawString(
				sharedUi.font, area.area.properties.displayName, srgbToLinear(rgb(238, 203, 127)),
				IntArray(0), transform.transformX(0.025f), lowerBar.minY, lowerBar.maxX / 2, lowerBar.maxY,
				transform.transformY(0.025f), transform.transformHeight(0.03f), 1
			)
		}

		renderer.drawString(
			sharedUi.font, state.menu.currentTab.getText(), srgbToLinear(rgb(132, 81, 37)),
			IntArray(0), transform.transformX(0.025f), upperBar.minY, upperBar.maxX / 2, upperBar.maxY,
			transform.transformY(0.945f), transform.transformHeight(0.035f), 1
		)

		renderTabName(renderer, transform, "Party", 0)
		renderTabName(renderer, transform, "Skills", 1)
		renderTabName(renderer, transform, "Inventory", 2)
		renderTabName(renderer, transform, "Map", 3)

		renderer.end()
	}

	private fun renderTabName(renderer: UiRenderer, transform: CoordinateTransform, text: String, index: Int) {
		val rect = transform.transform(0.7f, 0.85f - index * 0.07f, 0.3f, 0.06f)

		//val goldTint = srgbToLinear(rgba(215, 180, 43, 183))
		val goldTint = srgbToLinear(rgba(111, 92, 53, 183))
		renderer.fillColor(
			rect.minX, (rect.minY + rect.maxY) / 2, rect.maxX, rect.maxY, 0,
			Gradient(0, 0, rect.width, rect.height / 2, 0, goldTint, 0)
		)

		val upperTextColor = srgbToLinear(rgb(239, 237, 210))
		val lowerTextColor = srgbToLinear(rgb(214, 170, 98))
		val outline = intArrayOf(srgbToLinear(rgb(108, 82, 47)))

		renderer.drawString(
			sharedUi.font, text, upperTextColor, outline, rect.minX, rect.minY, rect.maxX, rect.maxY,
			rect.maxY - rect.height / 5, transform.transformHeight(0.04f), 1
		)
	}
}
