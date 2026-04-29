package mardek.renderer.title

import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.text.Vk2dFont
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.ui.TitleScreenContent
import mardek.renderer.MardekTextStyles
import mardek.renderer.RawRenderContext
import mardek.renderer.RenderContext
import mardek.renderer.menu.referenceTime
import mardek.renderer.save.renderSaveSelectionModal
import mardek.renderer.util.renderButton
import mardek.state.title.TitleScreenState
import mardek.state.util.Rectangle
import java.io.File
import java.nio.file.Files
import kotlin.math.roundToInt

private fun loadInfo(): TitleScreenContent {
	val inputPath = File("${Content.RESOURCES_DIRECTORY}/title-screen.bits").toPath()
	val input = BitInputStream(Files.newInputStream(inputPath))
	val titleScreenContent = BITSER.deserialize(TitleScreenContent::class.java, input)
	input.close()
	return titleScreenContent
}

internal val titleScreenInfo = loadInfo()

internal fun renderTitleScreen(
	context: RawRenderContext, fullRenderContext: RenderContext?,
	state: TitleScreenState, region: Rectangle,
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {

	val saveSelection = state.saveSelection
	if (saveSelection != null && fullRenderContext != null) {
		val framebuffers = fullRenderContext.framebuffers
		val backgroundRenderStage = context.pipelines.base.blur.addSourceStage(
			fullRenderContext.frame, framebuffers.blur, -1
		)
		context.pipelines.base.blur.addComputeStage(
			fullRenderContext.frame, fullRenderContext.perFrame.areaBlurDescriptors,
			framebuffers.blur, 4, 50, -1
		)
		val coreRegion = Rectangle(0, 0, backgroundRenderStage.width, backgroundRenderStage.height)
		renderCoreTitleScreen(context, backgroundRenderStage, state, coreRegion)

		fullRenderContext.currentStage = fullRenderContext.frame.swapchainStage

		val alpha = 0.9f
		fun addColor(brown: Float) = srgbToLinear(rgba(
			0.4f * brown * alpha, 0.25f * brown * alpha, 0.17f * brown * alpha, 1f
		))
		fun multiplyColor() = rgba(1f - alpha, 1f - alpha, 1f - alpha, 0f)

		context.pipelines.base.blur.addBatch(
			fullRenderContext.frame.swapchainStage,
			framebuffers.blur, fullRenderContext.perFrame.areaBlurDescriptors,
			region.minX.toFloat(), region.minY.toFloat(),
			(region.minX + region.width).toFloat(), (region.minY + region.height).toFloat(),
		).fixedColorTransform(addColor(0.4f), multiplyColor())

		val buttonFont = context.titleScreenBundle.getFont(titleScreenInfo.largeFont.index)
		val basicFont = context.titleScreenBundle.getFont(titleScreenInfo.basicFont.index)
		val fatFont = context.titleScreenBundle.getFont(titleScreenInfo.fatFont.index)

		val (colorBatch, glyphBatch) = renderSaveSelectionModal(
			fullRenderContext, basicFont, fatFont, buttonFont,
			saveSelection, false, region,
		)

		return Pair(colorBatch, glyphBatch)
	} else {
		return renderCoreTitleScreen(context, context.stage, state, region)
	}
}

private fun renderCoreTitleScreen(
	context: RawRenderContext, stage: Vk2dRenderStage, state: TitleScreenState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val imageBatch = context.pipelines.base.image.addBatch(stage, 12, context.titleScreenBundle)
	imageBatch.fillWithoutDistortion(
		region.minX.toFloat(), region.minY.toFloat(),
		region.boundX.toFloat(), region.boundY.toFloat(),
		titleScreenInfo.background.index
	)

	val colorBatch = context.pipelines.base.color.addBatch(stage, 100)
	val ovalBatch = context.pipelines.base.oval.addBatch(
		stage, context.perFrameDescriptorSet, 48
	)

	val buttonFont = context.titleScreenBundle.getFont(titleScreenInfo.largeFont.index)
	val basicFont = context.titleScreenBundle.getFont(titleScreenInfo.basicFont.index)
	val fancyTextBatch = context.pipelines.base.fancyText.addBatch(
		stage, 300, context.fancyTextStyleCache
	)
	val simpleTextBatch = context.pipelines.base.simpleText.addBatch(context.stage, 100, context.textStyleCache)

	for (style in arrayOf(MardekTextStyles.TitleScreen.TITLE_BACK, MardekTextStyles.TitleScreen.TITLE_FRONT)) {
		fancyTextBatch.drawString(
			"MARDEK", region.minX + region.height * 0.09f,
			region.minY + region.height * 0.25f,
			0f, region.height * 0.18f, buttonFont, style, TextAlignment.LEFT,
		)
	}

	run {
		fancyTextBatch.drawShadowedString(
			"Kotlin Edition", region.minX + region.height * 0.14f,
			region.minY + region.height * 0.38f, 0f, region.height * 0.07f, buttonFont,
			MardekTextStyles.TitleScreen.SUB_TITLE, TextAlignment.LEFT,
		)
	}

	val selectedButton = if (state.saveSelection != null) -2 else state.selectedButton
	state.newGameButton = renderLeftButton(
		region, colorBatch, ovalBatch, fancyTextBatch, buttonFont, "New Game",
		0.54f, selectedButton, 0
	)

	val availableCampaigns = state.availableCampaigns
	state.loadGameButton = renderLeftButton(
		region, colorBatch, ovalBatch, fancyTextBatch, buttonFont, "Load Game",
		0.64f, selectedButton, 1,
		availableCampaigns != null && availableCampaigns.isEmpty(),
	)
	state.musicPlayerButton = renderLeftButton(
		region, colorBatch, ovalBatch, fancyTextBatch, buttonFont, "Music Player",
		0.74f, selectedButton,2
	)
	state.quitButton = renderLeftButton(
		region, colorBatch, ovalBatch, fancyTextBatch, buttonFont, "Quit",
		0.84f, selectedButton, 3
	)

	if (state.newCampaignName != null) {
		simpleTextBatch.drawShadowedString(
			"Game name:", region.minX + 1.16f * region.height,
			region.minY + 0.5f * region.height, 0.04f * region.height, basicFont,
			MardekTextStyles.TitleScreen.GAME_NAME_LABEL, TextAlignment.LEFT,
		)
		val nameRectangle = renderRightButton(
			region, colorBatch, ovalBatch, fancyTextBatch, buttonFont, "",
			0.62f, state.selectedButton, -2, false,
		)
		state.beginButton = renderRightButton(
			region, colorBatch, ovalBatch, fancyTextBatch, buttonFont, "BEGIN",
			0.73f, state.selectedButton, 4, !state.isCampaignNameValid
		)

		val nameMargin = 22 * nameRectangle.height / 100
		val innerRectangle = Rectangle(
			nameRectangle.minX + nameMargin, nameRectangle.minY + nameMargin,
			nameRectangle.width - 2 * nameMargin, nameRectangle.height - 2 * nameMargin,
		)

		val fieldColor = srgbToLinear(rgb(61, 35, 18))
		colorBatch.fill(
			innerRectangle.minX, innerRectangle.minY,
			innerRectangle.maxX, innerRectangle.maxY, fieldColor,
		)
		val borderWidth = nameMargin / 3
		val borderColor = srgbToLinear(rgb(102, 51, 0))
		colorBatch.fill(
			innerRectangle.minX, innerRectangle.minY - borderWidth,
			innerRectangle.maxX, innerRectangle.minY - 1, borderColor,
		)
		colorBatch.fill(
			innerRectangle.minX, innerRectangle.boundY,
			innerRectangle.maxX, innerRectangle.maxY + borderWidth, borderColor,
		)
		val radius = innerRectangle.height * 0.4f + borderWidth
		ovalBatch.complex(
			nameRectangle.minX - nameMargin, innerRectangle.minY - borderWidth,
			innerRectangle.minX, innerRectangle.maxY + borderWidth,
			innerRectangle.minX + 1f * borderWidth, innerRectangle.minY + innerRectangle.height * 0.5f,
			borderWidth * 2.5f, 1.3f * radius,
			fieldColor, fieldColor, borderColor, borderColor, 0,
			0.75f, 0.75f, 1f, 1.1f,
		)
		ovalBatch.complex(
			innerRectangle.maxX, innerRectangle.minY - borderWidth,
			nameRectangle.maxX + nameMargin, innerRectangle.maxY + borderWidth,
			innerRectangle.maxX - 1f * borderWidth, innerRectangle.minY + innerRectangle.height * 0.5f,
			borderWidth * 2.5f, 1.3f * radius,
			fieldColor, fieldColor, borderColor, borderColor, 0,
			0.75f, 0.75f, 1f, 1.1f,
		)

		val blinkPeriod = 1000_000_000L
		val relativeTime = System.nanoTime() - referenceTime
		val showCaret = (relativeTime % blinkPeriod) >= blinkPeriod / 2
		simpleTextBatch.drawString(
			"${state.newCampaignName}${if (showCaret) "|" else ""}",
			innerRectangle.minX + innerRectangle.height * 0.2f,
			innerRectangle.maxY - innerRectangle.height * 0.25f, 0.025f * region.height,
			basicFont, MardekTextStyles.TitleScreen.GAME_NAME, TextAlignment.LEFT,
		)
	} else state.beginButton = null

	return Pair(colorBatch, simpleTextBatch)
}

private fun renderLeftButton(
	outerRegion: Rectangle, colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch, textBatch: Vk2dFancyTextBatch,
	font: Vk2dFont, text: String, relativeY: Float, selectedButton: Int, buttonIndex: Int, disabled: Boolean = false
): Rectangle? {
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
		colorBatch, ovalBatch, textBatch, font, true, text,
		true, selectedButton == buttonIndex && !disabled, disabled,
		rect, outlineWidth, textOffsetX, textBaseY, textHeight
	)
	return if (disabled) null else rect
}

private fun renderRightButton(
	outerRegion: Rectangle, colorBatch: Vk2dColorBatch, ovalBatch: Vk2dOvalBatch, textBatch: Vk2dFancyTextBatch,
	font: Vk2dFont, text: String, relativeY: Float, selectedButton: Int, buttonIndex: Int, disabled: Boolean,
): Rectangle {
	val rect = Rectangle(
		outerRegion.minX + 10 * outerRegion.height / 10,
		outerRegion.minY + (relativeY * outerRegion.height).roundToInt() - outerRegion.height / 11,
		outerRegion.height / 2,
		outerRegion.height / 11,
	)
	val outlineWidth = rect.height / 10
	val textOffsetX = rect.minX + outerRegion.height / 30
	val textBaseY = rect.maxY - outerRegion.height / 50
	val textHeight = outerRegion.height / 21
	renderButton(
		colorBatch, ovalBatch, textBatch, font, true, text,
		true, selectedButton == buttonIndex, disabled,
		rect, outlineWidth, textOffsetX, textBaseY, textHeight
	)
	return rect
}
