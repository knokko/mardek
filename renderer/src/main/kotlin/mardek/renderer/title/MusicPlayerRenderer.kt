package mardek.renderer.title

import com.github.knokko.boiler.utilities.ColorPacker.multiplyAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import mardek.renderer.RawRenderContext
import mardek.renderer.menu.determinePointerOffset
import mardek.renderer.util.renderButton
import mardek.state.title.MusicPlayerState
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration

internal fun renderMusicPlayer(
	context: RawRenderContext, state: MusicPlayerState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val backgroundImageBatch = context.pipelines.image.addBatch(context.stage, 2, context.titleScreenBundle)
	val colorBatch = context.pipelines.color.addBatch(context.stage, 100)
	val ovalBatch = context.pipelines.oval.addBatch(context.stage, context.perFrameDescriptorSet, 50)
	val imageBatch = context.pipelines.image.addBatch(context.stage, 100, context.titleScreenBundle)

	val textBatch = context.pipelines.simpleText.addBatch(context.stage, 200, context.textStyleCache)
	val fancyTextBatch = context.pipelines.fancyText.addBatch(context.stage, 750, context.fancyTextStyleCache)

	val upperLowerBarColor = srgbToLinear(rgb(24, 14, 10))
	val upperLowerBarEdgeColor = srgbToLinear(rgb(83, 55, 28))
	val upperLowerBarLineWidth = max(1, region.height / 200)
	val upperBarY = region.minY + 15 * region.height / 100
	val lowerBarY = region.minY + 85 * region.height / 100

	val scrollX = region.minX + region.height / 90
	val minScrollY = upperBarY + 1 - upperLowerBarLineWidth
	val maxScrollY = lowerBarY - 1 + upperLowerBarLineWidth
	colorBatch.fill(region.minX, minScrollY, scrollX, maxScrollY, upperLowerBarColor)

	val categoryMusicTracks = state.getMusicTracksInSelectedCategory(context.titleContent)
	run {
		val maxScrollHeight = 1 + maxScrollY - minScrollY
		val numTracks = categoryMusicTracks.size

		val minScrollOffset = maxScrollHeight * state.firstMusicIndexOnScreen / numTracks
		val maxScrollOffset = maxScrollHeight * min(numTracks, 1 + state.lastMusicIndexOnScreen) / numTracks

		colorBatch.fill(
			region.minX, minScrollY + minScrollOffset,
			scrollX, minScrollY + maxScrollOffset - 1,
			srgbToLinear(rgb(208, 193, 142)),
		)
	}

	colorBatch.fill(
		region.minX, region.minY, region.maxX, upperBarY - upperLowerBarLineWidth,
		upperLowerBarColor,
	)
	colorBatch.fill(
		scrollX + 1, upperBarY + 1 - upperLowerBarLineWidth, region.maxX, upperBarY,
		upperLowerBarEdgeColor,
	)

	colorBatch.fill(
		scrollX + 1, lowerBarY, region.maxX, lowerBarY + upperLowerBarLineWidth - 1,
		upperLowerBarEdgeColor,
	)
	colorBatch.fill(
		region.minX, lowerBarY + upperLowerBarLineWidth, region.maxX, region.maxY,
		upperLowerBarColor,
	)

	backgroundImageBatch.fillWithoutDistortion(
		scrollX + 1f, upperBarY + 1f,
		region.boundX.toFloat(), lowerBarY.toFloat(),
		context.titleContent.background.index,
	)

	val upperFont = context.titleScreenBundle.getFont(context.titleContent.largeFont.index)
	textBatch.drawString(
		"Music Player", region.minX + 0.04f * region.height, region.minY + 0.10f * region.height,
		0.048f * region.height, upperFont,
		MardekTextStyles.MusicPlayer.TITLE, TextAlignment.LEFT,
	)

	val basicFont = context.titleScreenBundle.getFont(context.titleContent.basicFont.index)
	val categoryName = if (state.selectedCategoryIndex >= 0) {
		context.titleContent.audio.musicCategories[state.selectedCategoryIndex].displayName
	} else "Showing All Tracks..."
	textBatch.drawString(
		categoryName, region.minX + 0.025f * region.height, region.maxY - 0.03f * region.height,
		0.027f * region.height, basicFont,
		MardekTextStyles.MusicPlayer.CATEGORY, TextAlignment.LEFT,
	)

	run {
		val backRegion = Rectangle(
			region.maxX - 8 * region.height / 20, region.maxY - region.height / 9,
			37 * region.height / 100, region.height / 13
		)
		val isSelected = backRegion.contains(state.mouseX, state.mouseY)
		renderButton(
			colorBatch, ovalBatch, fancyTextBatch, upperFont, true, "Back",
			true, isSelected, false, backRegion, region.height / 150,
			backRegion.minX + region.height / 45,
			backRegion.maxY - region.height / 52, region.height / 26,
		)
		state.backButton = backRegion
	}

	run {
		val arrowOffset = 0.003f * region.height * determinePointerOffset()
		val arrowSprite = context.titleContent.arrowHead
		val arrowScale = 0.0375f * region.height / arrowSprite.height

		val categories = context.titleContent.audio.musicCategories
		imageBatch.rotated(
			region.maxX - 0.03f * region.height - arrowOffset, region.minY + 0.075f * region.height,
			0f, arrowScale, arrowSprite.index, 0, -1,
		)

		for (categoryIndex in -1 until categories.size) {
			val noteSprite = if (categoryIndex == -1) context.titleContent.neutralMusicNote
			else categories[categoryIndex].icon

			val multiplyColor = if (categoryIndex == state.selectedCategoryIndex) -1
			else rgba(1f, 1f, 1f, 0.1f)
			imageBatch.rotated(
				region.maxX - region.height * (0.55f - 0.075f * categoryIndex),
				region.minY + 0.075f * region.height,
				0f, 0.08f * region.height / noteSprite.height,
				noteSprite.index, 0, multiplyColor,
			)
		}

		imageBatch.rotated(
			region.maxX - region.height * (0.1f + (1f + categories.size) * 0.075f) + arrowOffset,
			region.minY + 0.075f * region.height,
			180f, arrowScale, arrowSprite.index, 0, -1,
		)
	}

	for (trackIndex in state.firstMusicIndexOnScreen .. state.lastMusicIndexOnScreen) {
		if (trackIndex >= categoryMusicTracks.size) break
		val currentMusicTrack = categoryMusicTracks[trackIndex]

		val isUnlocked = context.state.saves.isMusicTrackUnlocked(currentMusicTrack)
		val lockedAlphaFactor = if (isUnlocked) 1f else 0.2f
		val lockedMultiplyColor = rgba(1f, 1f, 1f, lockedAlphaFactor)

		val baseMinX = region.minX + region.height / 12
		val baseMinY = upperBarY + region.height / 33 + (trackIndex - state.firstMusicIndexOnScreen) * 75 * region.height / 1000
		val baseMaxX = baseMinX + 5 * region.height / 9
		val baseMaxY = baseMinY + region.height / 23
		val isSelected = state.selectedMusicIndex == trackIndex
		var backgroundColor = if (isSelected) srgbToLinear(rgb(43, 85, 130))
		else srgbToLinear(rgba(125, 66, 16, 150))

		backgroundColor = multiplyAlpha(backgroundColor, lockedAlphaFactor)

		val radius = (1f + baseMaxY - baseMinY) * 0.5f
		ovalBatch.complex(
			baseMinX - (radius + 2).roundToInt(), baseMinY, baseMinX - 1, baseMaxY,
			baseMinX.toFloat(), baseMinY + radius, radius, radius,
			backgroundColor, backgroundColor, 0, 0, 0,
			1f, 1.1f, 12345f, 12345f,
		)
		colorBatch.gradient(
			baseMinX, baseMinY, baseMaxX, baseMaxY,
			backgroundColor, 0, backgroundColor
		)

		val textStyles = if (isSelected) MardekTextStyles.MusicPlayer.SELECTED_ENTRY
		else MardekTextStyles.MusicPlayer.DEFAULT_ENTRY

		val musicIndex = 1 + context.titleContent.audio.musicTracks.indexOf(currentMusicTrack)

		val musicDescription = "$musicIndex. ${if (isUnlocked) currentMusicTrack.displayName else "?????"}"
		for (textStyle in textStyles) {
			fancyTextBatch.drawString(
				musicDescription, baseMinX + 0.07f * region.height,
				baseMaxY - 0.007f * region.height, 0f, 0.024f * region.height,
				basicFont, textStyle.multiply(lockedMultiplyColor), TextAlignment.LEFT,
			)
		}

		val categoryNote = currentMusicTrack.category.icon

		val noteScale = 0.07f * region.height / categoryNote.height
		imageBatch.rotated(
			baseMinX + 0.025f * region.height, baseMinY + radius + 0.006f * region.height, -15f,
			noteScale, context.titleContent.musicNoteShadow.index, 0, lockedMultiplyColor,
		)
		imageBatch.rotated(
			baseMinX + 0.02f * region.height, baseMinY + radius, -15f, noteScale,
			categoryNote.index, 0, lockedMultiplyColor,
		)

		if (isSelected) {
			val pointerSprite = context.titleContent.crystalPointer
			val pointerScale = 0.027f * region.height / pointerSprite.height
			imageBatch.rotated(
				scrollX + (0.035f + 0.007f * determinePointerOffset()) * region.height,
				baseMinY + 0.016f * region.height,
				0f, pointerScale, pointerSprite.index,
				0, -1,
			)
		}
	}

	run {
		val barMaxX = region.maxX - region.height / 27
		val upperTextY = upperBarY + region.height / 10
		val barMinX = barMaxX - 8 * region.height / 14
		val pauseRegion = Rectangle(
			barMinX - region.height / 21, upperTextY + region.height / 150,
			region.height / 20, region.height / 25
		)
		state.pauseButton = pauseRegion

		val barMinY = upperTextY + region.height / 65
		val barMaxY = barMinY + region.height / 52

		colorBatch.fill(
			barMinX, barMinY, barMaxX, barMaxY,
			srgbToLinear(rgba(100, 60, 40, 200))
		)

		if (state.playingTrackDuration > Duration.ZERO) {
			val filledLength = (1 + barMaxX - barMinX) * state.timePlaying.inWholeNanoseconds /
					state.playingTrackDuration.inWholeNanoseconds
			val barFilledX = min(barMaxX, barMinX + Math.toIntExact(filledLength) - 1)
			colorBatch.fill(
				barMinX, barMinY, barFilledX, barMaxY,
				srgbToLinear(rgb(232, 187, 92))
			)

			val highGradientColor = srgbToLinear(rgb(250, 230, 180))
			val lowGradientColor = srgbToLinear(rgb(240, 210, 140))
			colorBatch.gradient(
				barMinX, barMinY + region.height / 300, barFilledX, barMinY + region.height / 80,
				lowGradientColor, lowGradientColor, highGradientColor,
			)
		}

		renderButton(
			colorBatch, ovalBatch, fancyTextBatch, basicFont, false, "", true,
			pauseRegion.contains(state.mouseX, state.mouseY), state.playingTrack == null,
			pauseRegion, region.height / 250, 0, 0, 0,
			disabledAlpha = 0.6f,
		)

		val leftTextX = barMinX + 0.018f * region.height
		textBatch.drawString(
			"now playing:", leftTextX, upperTextY.toFloat(), 0.032f * region.height,
			upperFont, MardekTextStyles.MusicPlayer.PLAYING_INFO, TextAlignment.LEFT,
		)

		val playingTrack = state.playingTrack
		if (playingTrack != null) {
			fancyTextBatch.drawString(
				playingTrack.displayName, leftTextX, barMaxY + 0.06f * region.height,
				0f, 0.04f * region.height, upperFont,
				MardekTextStyles.MusicPlayer.TRACK_NAME, TextAlignment.LEFT,
			)

			val boringFont = context.titleScreenBundle.getFont(context.titleContent.boringFont.index)
			val secondsPlayed = state.timePlaying.inWholeSeconds
			var secondsString = (secondsPlayed % 60).toString()
			if (secondsString.length == 1) secondsString = "0$secondsString"
			textBatch.drawString(
				"${secondsPlayed / 60}:$secondsString", barMaxX - 0.01f * region.height,
				upperTextY - 0.005f * region.height, 0.04f * region.height, boringFont,
				MardekTextStyles.MusicPlayer.PLAYING_INFO, TextAlignment.RIGHT,
			)
		}

		val icon = if (pauseRegion.contains(state.mouseX, state.mouseY)) {
			if (state.isPaused) context.titleContent.playMusicHoveredIcon
			else context.titleContent.pauseMusicHoveredIcon
		} else {
			if (state.isPaused) context.titleContent.playMusicIcon
			else context.titleContent.pauseMusicIcon
		}

		val marginX = 0.015f * region.height
		val marginY = 0.01f * region.height
		val multiplyColor = if (playingTrack == null) rgba(1f, 1f, 1f, 0.5f) else -1
		imageBatch.colored(
			pauseRegion.minX + marginX, pauseRegion.minY + marginY,
			pauseRegion.boundX - marginX, pauseRegion.boundY - marginY,
			icon.index, 0, multiplyColor,
		)
	}

	val timeSinceOpen = System.nanoTime() - state.openedAt
	if (timeSinceOpen < MusicPlayerState.FADE_IN_TIME) {
		val opacity = 1f - timeSinceOpen.toFloat() / MusicPlayerState.FADE_IN_TIME
		val alpha = (255f * opacity).roundToInt()
		if (alpha > 0) {
			context.pipelines.color.addBatch(context.stage, 2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0, 0, 0, alpha),
			)
		}
	}

	if (state.closedAt != 0L) {
		val timeSinceClose = System.nanoTime() - state.closedAt
		val opacity = timeSinceClose.toFloat() / MusicPlayerState.FADE_OUT_TIME
		val alpha = min(255, (255f * opacity).roundToInt())
		if (alpha > 0) {
			context.pipelines.color.addBatch(context.stage, 2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0, 0, 0, alpha),
			)
		}
	}

	return Pair(colorBatch, textBatch)
}
