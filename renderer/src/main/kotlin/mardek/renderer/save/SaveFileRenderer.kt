package mardek.renderer.save

import com.github.knokko.boiler.utilities.ColorPacker.changeAlpha
import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.text.TextAlignment
import com.github.knokko.vk2d.text.Vk2dFont
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import mardek.content.Content
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.animation.renderPortraitAnimation
import mardek.renderer.util.gradientWithBorder
import mardek.state.saves.SaveFile
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.math.max
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal fun renderSaveFile(
	colorBatch: Vk2dColorBatch,
	imageBatch: Vk2dImageBatch,
	partBatch: AnimationPartBatch,
	textBatch: Vk2dGlyphBatch,
	font: Vk2dFont,
	content: Content,
	saveFile: SaveFile?,
	isSelected: Boolean,
	isGray: Boolean,
	index: Int,
	region: Rectangle,
) {
	val borderWidth = max(1, region.height / 50)
	val borderColor = if (isSelected) srgbToLinear(rgb(165, 204, 254))
	else srgbToLinear(rgb(208, 193, 142))
	gradientWithBorder(
		colorBatch, region.minX, region.minY, region.maxX, region.maxY, borderWidth, borderWidth,
		borderColor, 0, 0,
		srgbToLinear(rgba(91, 68, 47, 200)),
	)

	if (isGray) {
		val gray = rgba(50, 50, 50, 100)
		colorBatch.fill(region.minX, region.minY, region.maxX, region.maxY, gray)
		return
	}

	if (isSelected) {
		colorBatch.gradient(
			region.minX + 1, region.minY + 1, region.maxX - 1, region.maxY - 1,
			0, changeAlpha(borderColor, 25), 0,
		)
	}

	if (saveFile != null) {
		val party = saveFile.info.party
		for ((index, memberID) in party.withIndex().reversed()) {
			val member = content.playableCharacters.find { it.id == memberID } ?: continue

			val animationContext = AnimationContext(
				renderRegion = Rectangle(
					region.minX - region.width / 10, region.minY - region.height,
					region.width, 2 * region.height,
				),
				renderTime = 0L,
				magicScale = content.portraits.magicScale,
				parentMatrix = Matrix3x2f().translate(
					region.minX + (1f + index) * region.height,
					region.minY - 0.15f * region.height
				).scale(-1f, 1f).scale(0.018f * region.height),
				parentColorTransform = null,
				partBatch = partBatch,
				noMask = content.battle.noMask,
				combat = null,
				portrait = member.portraitInfo,
				portraitExpression = "norm",
			)
			renderPortraitAnimation(content.portraits.animations, animationContext)
		}
	}

	if (isSelected) {
		val pointerY = region.minY + 0.3f * region.height
		val pointerHeight = 0.4f * region.height
		val pointerX = region.minX - 1.4f * pointerHeight
		val pointerImage = content.ui.pointer
		imageBatch.simpleScale(
			pointerX, pointerY,
			pointerHeight / pointerImage.height,
			pointerImage.index,
		)
	}

	val textColor = srgbToLinear(rgb(238, 203, 127))
	val shadowColor = srgbToLinear(rgb(61, 35, 18))
	val y1 = region.minY + 0.4f * region.height
	val y2 = region.minY + 0.8f * region.height

	fun drawString(
		text: String, relativeX: Float, y: Float,
		textHeight: Float = 0.22f * region.height,
		alignment: TextAlignment = TextAlignment.LEFT,
	) {
		val x = region.minX + relativeX * region.width
		val shadowOffset = 0.15f * textHeight
		textBatch.drawShadowedString(
			text, x, y, textHeight, font, textColor, 0, 0f,
			shadowColor, shadowOffset, shadowOffset, alignment,
		)
	}

	if (saveFile != null) {
		drawString(saveFile.campaignName, 0.33f, y1)

		val totalMinutes = saveFile.info.playTime.inWholeSeconds / 60
		var hourString = (totalMinutes / 60).toString()
		if (hourString.length == 1) hourString = "0$hourString"
		var minuteString = (totalMinutes % 60).toString()
		if (minuteString.length == 1) minuteString = "0$minuteString"
		drawString("$hourString:$minuteString", 0.372f, y2)

		val clockMinX = region.minX + 0.33f * region.width
		val clockMinY = region.minY + 0.52f * region.height
		val clockSize = 0.38f * region.height
		imageBatch.simple(
			clockMinX, clockMinY, clockMinX + clockSize, clockMinY + clockSize,
			content.ui.clock.index,
		)

		drawString("Ch ${saveFile.info.chapter}", 0.47f, y2)
		drawString("Lv ${saveFile.info.partyLevel}", 0.52f, y1)
		drawString(saveFile.info.areaName, 0.57f, y2)

		val saveTime = Instant.fromEpochMilliseconds(saveFile.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
		val dateString = saveTime.format(LocalDateTime.Format {
			dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
			char(' ')
			day()
			char('/')
			monthName(MonthNames.ENGLISH_ABBREVIATED)
			char('/')
			year()
			chars(" - ")
			amPmHour(Padding.NONE)
			char(':')
			minute()
			amPmMarker("am", "pm")
		})
		drawString(dateString, 0.6f, y1)
		drawString(
			(index + 1).toString(), 0.92f,
			region.maxY - 0.25f * region.height,
			0.5f * region.height,
		)
	} else {
		drawString(
			"Create new save", 0.5f, region.minY + 0.7f * region.height,
			0.4f * region.height, TextAlignment.CENTERED,
		)
	}
}

internal const val SAVE_FILE_ASPECT_RATIO = 12
