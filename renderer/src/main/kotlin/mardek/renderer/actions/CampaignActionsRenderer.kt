package mardek.renderer.actions

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionShowChapterName
import mardek.content.action.FixedActionNode
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.util.Rectangle

internal fun renderCampaignActions(
	context: RenderContext, actions: CampaignActionsState, region: Rectangle
): Pair<Vk2dColorBatch?, Vk2dGlyphBatch?> {
	val colorBatch: Vk2dColorBatch? = null
	var textBatch: MardekGlyphBatch? = null

	val renderTime = System.nanoTime()
	val relativeTime = renderTime - actions.currentNodeStartTime
	val node = actions.node

	if (node is FixedActionNode) {
		val action = node.action
		if (action is ActionShowChapterName) {
			if (relativeTime < ActionShowChapterName.TOTAL_DURATION) {
				textBatch = context.addFancyTextBatch(100)
				renderChapterNameAndNumber(context, textBatch, action, relativeTime, region)
			} else {
				actions.finishedAnimationNode = true
			}
		}

		if (action is ActionPlayCutscene) {
			renderCutscene(context, actions, action, renderTime, region) { capacity ->
				textBatch = context.addFancyTextBatch(capacity)
				textBatch
			}
		}
	}

	return Pair(colorBatch, textBatch)
}
