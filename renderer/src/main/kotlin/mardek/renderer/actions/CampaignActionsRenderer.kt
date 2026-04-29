package mardek.renderer.actions

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionShowChapterName
import mardek.content.action.FixedActionNode
import mardek.renderer.RenderContext
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.util.Rectangle

internal fun renderCampaignActions(
	context: RenderContext, actions: CampaignActionsState, region: Rectangle
): Pair<Vk2dColorBatch?, Vk2dSimpleTextBatch?> {
	var colorBatch: Vk2dColorBatch? = null
	var simpleTextBatch: Vk2dSimpleTextBatch? = null

	val renderTime = System.nanoTime()
	val relativeTime = renderTime - actions.currentNodeStartTime
	val node = actions.node

	if (node is FixedActionNode) {
		val action = node.action
		if (action is ActionShowChapterName) {
			if (relativeTime < ActionShowChapterName.TOTAL_DURATION) {
				colorBatch = context.addColorBatch(36)
				simpleTextBatch = context.addTextBatch(100)
				renderChapterNameAndNumber(
					context, simpleTextBatch,
					context.addFancyTextBatch(100), action, relativeTime, region,
				)
			} else {
				actions.finishedAnimationNode = true
			}
		}

		if (action is ActionPlayCutscene) {
			colorBatch = renderCutscene(context, actions, action, renderTime, region) { capacity ->
				context.addFancyTextBatch(capacity)
			}
		}
	}

	return Pair(colorBatch, simpleTextBatch)
}
