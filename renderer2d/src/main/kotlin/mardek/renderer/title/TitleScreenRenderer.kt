package mardek.renderer.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import mardek.content.ui.TitleScreenContent
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderResources
import mardek.renderer.util.renderButton
import mardek.state.title.TitleScreenState
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

private fun loadInfo(): TitleScreenContent {
	val input = RenderResources::class.java.classLoader.getResourceAsStream("mardek/game/title-screen.bits")!!
	return Bitser(false).deserialize(
		TitleScreenContent::class.java,
		BitInputStream(input),
		Bitser.BACKWARD_COMPATIBLE
	)
}

private val info = loadInfo()

private var firstFrame = true
fun renderTitleScreen(context: RawRenderContext, state: TitleScreenState, region: Rectangle): Vk2dColorBatch {
	if (firstFrame) {
		// TODO get rid of this
		context.resources.postInit(context.titleScreenBundle, info)
		firstFrame = false
	}

	val imageBatch = context.resources.imagePipeline.addBatch(context.frame, 12)
	imageBatch.simple(
		region.minX, region.minY, region.maxX, region.maxY,
		context.titleScreenBundle.getImageDescriptor(info.background.index)
	)

	run {
		val renderHeight = region.height / 5
		imageBatch.simple(
			region.minX + region.height / 20, region.minY + region.height / 4 - renderHeight,
			region.minX + region.height / 20 + renderHeight * info.title.width / info.title.height - 1,
			region.minY + region.height / 4,
			context.titleScreenBundle.getImageDescriptor(info.title.index)
		)
	}

	val colorBatch = context.resources.colorPipeline.addBatch(context.frame, 100)
	val ovalBatch = context.resources.ovalPipeline.addBatch(context.frame, 48)

	val buttonFont = context.titleScreenBundle.getFont(info.smallFont.index)
	val glyphBatch = context.resources.glyphPipeline.addBatch(
		context.frame, 100, buttonFont, context.resources.textBuffer.renderDescriptorSet
	)

	state.newGameButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, "New Game",
		0.54f, state.selectedButton, 0
	)
	state.loadGameButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, "Load Game",
		0.64f, state.selectedButton, 1
	)
	state.musicPlayerButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, "Music Player",
		0.74f, state.selectedButton,2
	)
	state.quitButton = renderButton(
		region, colorBatch, ovalBatch, glyphBatch, "Quit",
		0.84f, state.selectedButton, 3
	)

	return colorBatch
}

private fun renderButton(
	outerRegion: Rectangle, colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch, glyphBatch: Vk2dGlyphBatch,
	text: String, relativeY: Float, selectedButton: Int, buttonIndex: Int
): Rectangle {
	val rect = Rectangle(
		outerRegion.minX + 3 * outerRegion.height / 10,
		outerRegion.minY + (relativeY * outerRegion.height).roundToInt() - outerRegion.height / 12,
		outerRegion.height / 2,
		outerRegion.height / 12,
	)
	val outlineWidth = rect.height / 10
	val textOffsetX = rect.minX + outerRegion.height / 200
	val textBaseY = rect.maxY - outerRegion.height / 50
	val textHeight = outerRegion.height / 22
	renderButton(
		colorBatch, ovalBatch, glyphBatch, true, text,
		selectedButton == buttonIndex,
		rect, outlineWidth, textOffsetX, textBaseY, textHeight
	)
	return rect
}
