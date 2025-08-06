package mardek.renderer

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.area.renderCurrentArea
import mardek.state.ingame.InGameState
import mardek.state.util.Rectangle

internal fun renderInGame(context: RenderContext, state: InGameState, region: Rectangle): Vk2dColorBatch {

	var titleColorBatch: Vk2dColorBatch? = null
	val area = state.campaign.currentArea
	if (area != null) titleColorBatch = renderCurrentArea(context, area, region)

	if (titleColorBatch == null) titleColorBatch = context.addColorBatch(52) // TODO Check right capacity
	return titleColorBatch
}
