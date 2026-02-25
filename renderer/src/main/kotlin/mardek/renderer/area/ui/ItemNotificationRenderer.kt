package mardek.renderer.area.ui

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import mardek.renderer.area.AreaRenderContext
import mardek.state.ingame.area.AreaSuspensionActions
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun renderActionsItemNotification(areaContext: AreaRenderContext) {
	areaContext.apply {
		val suspension = state.suspension
		if (suspension !is AreaSuspensionActions) return

		val notification = suspension.actions.itemNotification ?: return
		val fadeInDuration = 500.milliseconds
		val displayDuration = 2.seconds
		val fadeOutDuration = 1.seconds

		var opacity = 0.0
		var passedTime = state.currentTime - notification.timestamp
		if (passedTime < fadeInDuration) {
			opacity = passedTime / fadeInDuration
		} else {
			passedTime -= fadeInDuration
			if (passedTime < displayDuration) {
				opacity = 1.0
			} else {
				passedTime -= displayDuration
				if (passedTime < fadeOutDuration) {
					opacity = 1.0 - passedTime / fadeOutDuration
				}
			}
		}

		if (opacity <= 0.0) return

		val barY = region.minY + region.height / 12
		val alpha = (255 * opacity).roundToInt()
		uiColorBatch.fill(
			region.minX, region.minY, region.maxX, barY - 1,
			srgbToLinear(rgba(19, 11, 8, alpha)),
		)

		uiColorBatch.fill(
			region.minX, barY, region.maxX, barY + region.height / 500,
			srgbToLinear(rgba(73, 52, 37, alpha)),
		)

		itemNotificationBatch.draw(
			notification.stack.item.sprite, region.minX + region.height / 60, region.minY + region.height / 50,
			region.height / 160, opacity = opacity.toFloat() * 0.7f,
		)

		var text = notification.operation.prefix + notification.stack.item.displayName
		if (notification.stack.amount != 1) text += " x${notification.stack.amount}"
		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		textBatch.drawString(
			text, region.minX + 0.13f * region.height, region.minY + 0.06f * region.height,
			0.025f * region.height, font,
			srgbToLinear(rgba(238, 203, 127, alpha))
		)
	}
}
