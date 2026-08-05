package mardek.renderer.area

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import mardek.renderer.RenderContext
import mardek.renderer.area.ui.renderActionBackgroundImage
import mardek.renderer.area.ui.renderActionFlash
import mardek.renderer.area.ui.renderActionOverlayColor
import mardek.renderer.area.ui.renderActionsItemNotification
import mardek.renderer.area.ui.renderAreaDialogue
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle

internal fun renderCurrentArea(
	context: RenderContext, state: AreaState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dSimpleTextBatch> {
	val areaContext = AreaRenderContext.create(context, state, region)

	collectAreaObjects(areaContext)
	collectAreaCharacters(areaContext)
	renderTiles(areaContext)
	collectIncomingBattleIndicator(areaContext)

	for (job in areaContext.renderJobs) job.addToBatch(areaContext)

	renderObtainedGold(areaContext)
	renderAreaLights(areaContext)
	renderAreaAmbience(areaContext)
	renderActionBackgroundImage(areaContext)
	renderAreaActionEffects(areaContext)
	renderAreaDialogue(areaContext)
	renderActionsItemNotification(areaContext)
	renderActionFlash(areaContext)
	renderActionOverlayColor(areaContext)
	renderAreaFadeEffects(areaContext)
	renderAreaIncomingBattleFlicker(areaContext)

	return Pair(areaContext.uiColorBatch, areaContext.simpleTextBatch)
}
