package mardek.renderer.area

import com.github.knokko.vk2d.text.TextAlignment
import mardek.renderer.MardekTextStyles
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun renderObtainedGold(areaContext: AreaRenderContext) {
	areaContext.run {
		val obtainedGold = state.obtainedGold ?: return
		val baseX = region.minX + tileSize * obtainedGold.chestX + region.width / 2 - cameraX
		var baseY = region.minY + tileSize * obtainedGold.chestY + region.height / 2 - cameraY// - 4 * scale
		baseY += areaTimings.interpolate(
			obtainedGold.shownSince, 3.5f * scale,
			250.milliseconds, -5f * scale, true,
		).roundToInt()
		val opacity = areaTimings.interpolate(
			obtainedGold.shownSince.virtualAdd(1.seconds), 1f,
			150.milliseconds, 0f, true
		)
		if (opacity <= 0f) return
		val alpha = (255f * opacity).roundToInt()

		goldSpriteBatch.draw(
			context.content.ui.goldIcon,
			baseX - tileSize * 19 / 32,
			baseY - tileSize * 14 / 32,
			scale / 2f, opacity = opacity
		)

		val font = context.bundle.getFont(context.content.fonts.basic1.index)
		simpleTextBatch.drawString(
			"+${state.obtainedGold!!.amount}", baseX.toFloat(), baseY.toFloat(), 6f * scale,
			font, MardekTextStyles.chestGold(alpha), TextAlignment.LEFT,
		)
	}
}
