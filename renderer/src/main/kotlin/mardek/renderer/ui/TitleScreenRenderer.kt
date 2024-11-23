package mardek.renderer.ui

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.ui.renderer.Gradient
import com.github.knokko.ui.renderer.UiRenderer
import mardek.renderer.StateRenderer
import mardek.state.title.AbsoluteRectangle
import mardek.state.title.TitleScreenState

class TitleScreenRenderer(
	boiler: BoilerInstance,
	private val sharedUI: SharedUiResources,
	private val state: TitleScreenState,
): StateRenderer(boiler) {

	override fun render(recorder: CommandRecorder, targetImage: VkbImage, frameIndex: Int) {
		val renderer = sharedUI.uiRenderers[frameIndex]
		renderer.begin(recorder, targetImage)
		renderer.drawImage(sharedUI.bc1Images[0], 0, 0, targetImage.width, targetImage.height)

		val transform = CoordinateTransform.create(SpaceLayout.GrowRight, targetImage.width, targetImage.height)

		run {
			val titleRect = transform.transform(0.05f, 0.67f, 2f, 0.3f)
			val darkColor = srgbToLinear(rgb(90, 51, 17))
			val mediumColor = srgbToLinear(rgb(180, 90, 65))
			val lightColor = srgbToLinear(rgb(233, 194, 186))
			val outline1 = srgbToLinear(rgb(68, 51, 34))
			val outline2 = srgbToLinear(rgb(190, 144, 95))
			val titleOutline = intArrayOf(outline1, outline1, outline2, outline2)

			val baseY = transform.transformY(0.77f)
			val heightA = transform.transformHeight(0.18f)

			val darkHeight = heightA * 3 / 10
			val lightHeight = heightA * 2 / 10
			val gy = baseY - titleRect.minY - heightA + titleOutline.size
			renderer.drawString(
				sharedUI.font, "MARDEK", darkColor, titleOutline,
				titleRect.minX, titleRect.minY, titleRect.maxX, titleRect.maxY, baseY, heightA, 2,
				Gradient(0, gy, titleRect.width, darkHeight, mediumColor, mediumColor, darkColor),
				Gradient(0, gy + darkHeight, titleRect.width, lightHeight, lightColor, lightColor, mediumColor),
				Gradient(0, gy + darkHeight + lightHeight, titleRect.width, lightHeight, mediumColor, mediumColor, lightColor),
				Gradient(0, gy + darkHeight + 2 * lightHeight, titleRect.width, darkHeight, darkColor, darkColor, mediumColor)
			)
		}

		run {
			val subtitleRect = transform.transform(0.3f, 0.58f, 2f, 0.2f)
			val heightA = transform.transformHeight(0.07f)
			val baseY = transform.transformY(0.63f)
			val darkTint = srgbToLinear(rgb(185, 131, 60))
			renderer.drawString(
				sharedUI.font, "Revival project", srgbToLinear(rgb(242, 183, 113)), IntArray(0),
				subtitleRect.minX, subtitleRect.minY, subtitleRect.maxX, subtitleRect.maxY, baseY, heightA, 1,
				Gradient(0, baseY - subtitleRect.minY - heightA / 3, 1000, heightA, darkTint, darkTint, darkTint)
			)
		}

		state.newGameButton = renderButton(transform, renderer, "New Game", 0.46f, 0)
		state.loadGameButton = renderButton(transform, renderer, "Load Game", 0.36f, 1)
		state.musicPlayerButton = renderButton(transform, renderer, "Music Player", 0.26f, 2)
		state.quitButton = renderButton(transform, renderer, "Quit", 0.16f, 3)

		renderer.end()
	}

	private fun renderButton(
		transform: CoordinateTransform, renderer: UiRenderer, text: String, minY: Float, buttonIndex: Int
	): AbsoluteRectangle {
		val rect = transform.transform(0.3f, minY, 0.5f, 0.08f)
		val outlineWidth = transform.transformHeight(0.005f)

		val borderLight = srgbToLinear(rgb(255, 204, 153))
		val borderHoverLight = srgbToLinear(rgb(152, 190, 222))
		val borderDark = srgbToLinear(rgb(101, 50, 1))
		val borderHoverDark = srgbToLinear(rgb(31, 68, 122))
		val innerDark = srgbToLinear(rgb(169, 71, 6))
		val innerLight = srgbToLinear(rgb(183, 105, 53))

		val innerLeft = srgbToLinear(rgba(169, 67, 1, 204))
		val innerHoverLeft = srgbToLinear(rgba(6, 82, 155, 184))
		val innerRight = srgbToLinear(rgba(68, 45, 33, 108))
		val innerHoverRight = rgba(0, 0, 0, 0)

		val rightAlpha = 200

		fun fillColors(borderTopLeft: Int, borderBottomRight: Int, left: Int, right: Int) {
			renderer.fillColor(rect.minX, rect.minY, rect.maxX, rect.minY + outlineWidth - 1, borderTopLeft)
			renderer.fillColor(rect.minX, rect.minY, rect.minX + outlineWidth - 1, rect.maxY, borderTopLeft)
			renderer.fillColor(
				rect.minX + outlineWidth, rect.minY + outlineWidth, rect.maxX, rect.maxY, borderBottomRight,
				Gradient(0, 0, rect.width - 2 * outlineWidth, rect.height - 2 * outlineWidth, left, right, left)
			)
		}
		fillColors(borderLight, borderDark, innerLeft, innerRight)

		val upperTextColor = srgbToLinear(rgb(248, 232, 194))
		val lowerTextColor = srgbToLinear(rgb(238, 203, 127))

		val outlineColors = intArrayOf(srgbToLinear(rgb(112, 64, 33)))
		val textOffsetX = rect.minX + transform.transformWidth(0.05f)
		val textBaseY = rect.maxY - transform.transformHeight(0.02f)
		val textHeight = transform.transformHeight(0.045f)
		renderer.drawString(
			sharedUI.font, text, lowerTextColor, outlineColors, textOffsetX, rect.minY, rect.maxX, rect.maxY,
			textBaseY, textHeight, 1,
			Gradient(0, 0, rect.width, rect.height / 2, upperTextColor, upperTextColor, upperTextColor)
		)

		if (state.selectedButton == buttonIndex) {
			fillColors(borderHoverDark, borderHoverLight, innerHoverLeft, innerHoverRight)
		}

		return rect
	}
}
