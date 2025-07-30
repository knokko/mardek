package mardek.renderer.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.text.Vk2dFont
import mardek.content.ui.TitleScreenContent
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderContext
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

private val info = loadInfo()

fun renderTitleScreen(context: RawRenderContext, state: TitleScreenState, region: Rectangle): Vk2dColorBatch {
	val imageBatch = context.pipelines.image.addBatch(context.frame, 12)
	imageBatch.simple(
		region.minX, region.minY, region.maxX, region.maxY,
		context.titleScreenBundle.getImageDescriptor(info.background.index)
	)

	val colorBatch = context.pipelines.color.addBatch(context.frame, 100)
	val ovalBatch = context.pipelines.oval.addBatch(
		context.frame, context.perFrameDescriptorSet, 48
	)

	val buttonFont = context.titleScreenBundle.getFont(info.largeFont.index)
	val simpleFont = context.titleScreenBundle.getFont(info.smallFont.index)
	// TODO Ditch simpleFont entirely?
	val glyphBatch = context.pipelines.text.addBatch(
		context.frame, 100, context.recorder,
		context.textBuffer, context.perFrameDescriptorSet
	)

	glyphBatch.drawString(
		"MARDEK", region.minX + region.height * 0.09f, region.minY + region.height * 0.25f,
		region.height * 0.18f, buttonFont, srgbToLinear(rgb(155, 80, 45))
	)
	glyphBatch.drawShadowedString(
		"Kotlin Edition", region.minX + region.height * 0.14f, region.minY + region.height * 0.38f,
		region.height * 0.07f, buttonFont, srgbToLinear(rgb(242, 183, 113)),
		0, 0f, srgbToLinear(rgb(91, 63, 30)),
		region.height * 0.005f, region.height * 0.005f
	)

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

	return colorBatch
}

private fun renderButton(
	outerRegion: Rectangle, colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch, glyphBatch: Vk2dGlyphBatch,
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
		selectedButton == buttonIndex,
		rect, outlineWidth, textOffsetX, textBaseY, textHeight
	)
	return rect
}
