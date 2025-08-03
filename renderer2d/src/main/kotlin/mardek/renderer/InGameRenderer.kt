package mardek.renderer

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.area.renderCurrentArea
import mardek.state.ingame.InGameState
import mardek.state.util.Rectangle

internal fun renderInGame(context: RenderContext, state: InGameState, region: Rectangle): Vk2dColorBatch {
	val titleColorBatch = context.addColorBatch()

	val area = state.campaign.currentArea
	if (area != null) renderCurrentArea(context, area, region, titleColorBatch)

	return titleColorBatch
}
