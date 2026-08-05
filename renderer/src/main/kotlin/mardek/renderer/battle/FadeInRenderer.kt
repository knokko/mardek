package mardek.renderer.battle

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.state.util.Rectangle
import kotlin.time.Duration.Companion.seconds

internal fun renderBattleFadeIn(
	battleContext: BattleRenderContext, colorBatch: Vk2dColorBatch, region: Rectangle
) {
	battleContext.run {
		val fadeAlpha = context.timing.interpolate(
			battle.startTime, 255,
			1.seconds, 0, true
		)
		if (fadeAlpha > 0) {
			colorBatch.fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0, 0, 0, fadeAlpha),
			)
		}
	}
}
