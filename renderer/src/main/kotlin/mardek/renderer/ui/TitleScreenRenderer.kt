package mardek.renderer.ui

import com.github.knokko.boiler.utilities.ColorPacker.*
import com.github.knokko.text.placement.TextAlignment
import com.github.knokko.ui.renderer.Gradient
import mardek.renderer.RenderContext
import mardek.renderer.StateRenderer
import mardek.state.title.AbsoluteRectangle
import mardek.state.title.TitleScreenState

class TitleScreenRenderer(
	private val state: TitleScreenState,
): StateRenderer() {

	override fun render(context: RenderContext) {
		context.uiRenderer.begin(context.recorder, context.targetImage)
		context.uiRenderer.beginBatch()

		context.uiRenderer.drawImage(
			context.resources.bcImages[context.content.ui.titleScreenBackground.index],
			0, 0, context.targetImage.width, context.targetImage.height
		)

		val transform = CoordinateTransform.create(
			SpaceLayout.GrowRight, context.targetImage.width, context.targetImage.height
		)

		run {
			val height = 0.2f
			// TODO Export title with higher quality
			val image = context.resources.bcImages[context.content.ui.titleScreenTitle.index]
			val titleRect = transform.transform(0.05f, 0.77f, height * image.width / image.height, height)
			context.uiRenderer.drawImage(image, titleRect.minX, titleRect.minY, titleRect.maxX, titleRect.maxY)
		}

		run {
			val subtitleRect = transform.transform(0.3f, 0.58f, 2f, 0.2f)
			val heightA = transform.transformHeight(0.07f)
			val baseY = transform.transformY(0.63f)
			val darkTint = srgbToLinear(rgb(185, 131, 60))
			context.uiRenderer.drawString(
				context.resources.font, "Revival project", srgbToLinear(rgb(242, 183, 113)), IntArray(0),
				subtitleRect.minX, subtitleRect.minY, subtitleRect.maxX, subtitleRect.maxY, baseY, heightA, 1, TextAlignment.DEFAULT,
				Gradient(0, baseY - subtitleRect.minY - heightA / 3, 1000, heightA, darkTint, darkTint, darkTint)
			)
		}

		state.newGameButton = renderButton(transform, context, "New Game", 0.46f, 0)
		state.loadGameButton = renderButton(transform, context, "Load Game", 0.36f, 1)
		state.musicPlayerButton = renderButton(transform, context, "Music Player", 0.26f, 2)
		state.quitButton = renderButton(transform, context, "Quit", 0.16f, 3)

		context.uiRenderer.endBatch()
		context.uiRenderer.end()
	}

	private fun renderButton(
		transform: CoordinateTransform, context: RenderContext, text: String, minY: Float, buttonIndex: Int
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
			context.uiRenderer.fillColor(rect.minX, rect.minY, rect.maxX, rect.minY + outlineWidth - 1, borderTopLeft)
			context.uiRenderer.fillColor(rect.minX, rect.minY, rect.minX + outlineWidth - 1, rect.maxY, borderTopLeft)
			context.uiRenderer.fillColor(
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
		context.uiRenderer.drawString(
			context.resources.font, text, lowerTextColor, outlineColors, textOffsetX, rect.minY, rect.maxX, rect.maxY,
			textBaseY, textHeight, 1, TextAlignment.DEFAULT,
			Gradient(0, 0, rect.width, rect.height / 2, upperTextColor, upperTextColor, upperTextColor)
		)

		if (state.selectedButton == buttonIndex) {
			fillColors(borderHoverDark, borderHoverLight, innerHoverLeft, innerHoverRight)
		}

		return rect
	}
}
