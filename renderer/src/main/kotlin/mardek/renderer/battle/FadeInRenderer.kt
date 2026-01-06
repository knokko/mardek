package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.state.util.Rectangle

internal fun renderBattleFadeIn(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch, region: Rectangle
) {
	battleContext.run {
		val fadeInTime = 250_000_000L
		val relativeTime = renderTime - battle.startTime
		if (relativeTime < fadeInTime) {
			val fade = 1f - relativeTime.toFloat() / fadeInTime
			if (fade > 0.001f) {
				colorBatch.fill(
					region.minX, region.minY, region.maxX, region.maxY,
					rgba(0f, 0f, 0f, fade),
				)
			}
		}
	}
}
