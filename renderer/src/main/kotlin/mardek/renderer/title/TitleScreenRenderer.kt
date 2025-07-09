package mardek.renderer.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dFont
import mardek.content.ui.TitleScreenContent
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.renderer.util.renderButton
import mardek.state.title.TitleScreenState
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

private fun loadInfo(): TitleScreenContent {
	val input = RenderContext::class.java.classLoader.getResourceAsStream("mardek/game/title-screen.bits")!!
	return Bitser(false).deserialize(
		TitleScreenContent::class.java,
		BitInputStream(input),
		Bitser.BACKWARD_COMPATIBLE
	)
}

internal val titleScreenInfo = loadInfo()

internal fun renderTitleScreen(
	context: RawRenderContext, state: TitleScreenState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {
	val imageBatch = context.pipelines.base.image.addBatch(context.stage, 12, context.titleScreenBundle)
	imageBatch.fillWithoutDistortion(
		region.minX.toFloat(), region.minY.toFloat(),
		region.boundX.toFloat(), region.boundY.toFloat(),
		titleScreenInfo.background.index
	)

	val colorBatch = context.pipelines.base.color.addBatch(context.stage, 100)
	val ovalBatch = context.pipelines.base.oval.addBatch(
		context.stage, context.perFrameDescriptorSet, 48
	)

	val buttonFont = context.titleScreenBundle.getFont(titleScreenInfo.largeFont.index)
	val glyphBatch = context.pipelines.fancyText.addBatch(
		context.stage, 200, context.recorder,
		context.textBuffer, context.perFrameDescriptorSet
	)

	run {
		val outerColor = srgbToLinear(rgb(107, 53, 4))
		val quarterColor = srgbToLinear(rgb(185, 93, 68))
		val middleColor = srgbToLinear(rgb(230, 187, 178))
		val innerBorderColor = srgbToLinear(rgb(68, 51, 34))
		val outerBorderColor = srgbToLinear(rgb(190, 144, 95))
		val borderWidth = 0.04f * region.height
		glyphBatch.drawFancyBorderedString(
			"MARDEK", region.minX + region.height * 0.09f, region.minY + region.height * 0.25f,
			region.height * 0.18f, buttonFont, outerColor,
			innerBorderColor, borderWidth, TextAlignment.LEFT,
			quarterColor, middleColor, quarterColor, outerColor,
			0.3f, 0.4f, 0.5f, 1f,
			innerBorderColor, outerBorderColor, outerBorderColor, outerBorderColor,
			0.25f * borderWidth, 0.25f * borderWidth, 12345f, 12345f
		)
	}

	run {
		val shadowColor = srgbToLinear(rgb(91, 63, 30))
		val lowerColor = srgbToLinear(rgb(184, 130, 60))
		val upperColor = srgbToLinear(rgb(241, 182, 113))
		glyphBatch.drawFancyShadowedString(
			"Kotlin Edition", region.minX + region.height * 0.14f, region.minY + region.height * 0.38f,
			region.height * 0.07f, buttonFont, lowerColor, 0, 0f,
			lowerColor, upperColor, upperColor, upperColor,
			0.3f, 0.3f, 12345f, 12345f,
			shadowColor, region.height * 0.005f, region.height * 0.005f, TextAlignment.LEFT,
		)
	}

	state.newGameButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, buttonFont, "New Game",
		0.54f, state.selectedButton, 0
	)
	state.loadGameButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, buttonFont, "Load Game",
		0.64f, state.selectedButton, 1
	)
	state.musicPlayerButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, buttonFont, "Music Player",
		0.74f, state.selectedButton,2
	)
	state.quitButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, buttonFont, "Quit",
		0.84f, state.selectedButton, 3
	)

	return Pair(colorBatch, glyphBatch)
}

private fun renderButton(
	outerRegion: Rectangle, colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch, glyphBatch: MardekGlyphBatch,
	font: Vk2dFont, text: String, relativeY: Float, selectedButton: Int, buttonIndex: Int
): Rectangle {
	val rect = Rectangle(
		outerRegion.minX + 3 * outerRegion.height / 10,
		outerRegion.minY + (relativeY * outerRegion.height).roundToInt() - outerRegion.height / 12,
		outerRegion.height / 2,
		outerRegion.height / 12,
	)
	val outlineWidth = rect.height / 10
	val textOffsetX = rect.minX + outerRegion.height / 30
	val textBaseY = rect.maxY - outerRegion.height / 50
	val textHeight = outerRegion.height / 22
	renderButton(
		colorBatch, ovalBatch, glyphBatch, font, true, text,
		true, selectedButton == buttonIndex, false,
		rect, outlineWidth, textOffsetX, textBaseY, textHeight
	)
	return rect
}
