package mardek.renderer.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.content.ui.TitleScreenContent
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderResources
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

fun renderTitleScreen(context: RawRenderContext, state: TitleScreenState, region: Rectangle): Vk2dColorBatch {
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

	val colorBatch = context.resources.colorPipeline.addBatch(context.frame, 36)

	state.newGameButton = renderButton(region, colorBatch, "New Game", 0.54f, 0)
	state.loadGameButton = renderButton(region, colorBatch, "Load Game", 0.64f, 1)
	state.musicPlayerButton = renderButton(region, colorBatch, "Music Player", 0.74f, 2)
	state.quitButton = renderButton(region, colorBatch, "Quit", 0.84f, 3)

	return colorBatch
}

private fun renderButton(
	outerRegion: Rectangle, colorBatch: Vk2dColorBatch,
	text: String, relativeY: Float, index: Int
): Rectangle {
	val rect = Rectangle(
		outerRegion.minX + 3 * outerRegion.height / 10,
		outerRegion.minY + (relativeY * outerRegion.height).roundToInt() - outerRegion.height / 12,
		outerRegion.height / 2,
		outerRegion.height / 12,
	)
	val outlineWidth = outerRegion.height / 200
	val textOffsetX = rect.minX + outerRegion.height / 200
	val textBaseY = rect.maxY - outerRegion.height / 50
	val textHeight = outerRegion.height / 22
	colorBatch.fill(rect.minX, rect.minY, rect.maxX, rect.maxY, rgb(100, 100, 100))
//	renderButton(
//		context.uiRenderer, context.resources.font, true, text,
//		state.selectedButton == buttonIndex,
//		rect, outlineWidth, textOffsetX, textBaseY, textHeight
//	)
	return rect
}
