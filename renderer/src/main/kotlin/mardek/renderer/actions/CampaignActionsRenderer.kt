package mardek.renderer.actions

import com.github.knokko.boiler.utilities.ColorPacker.alpha
import com.github.knokko.boiler.utilities.ColorPacker.interpolateColors
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import mardek.content.action.ActionEndOfChapter
import mardek.content.action.ActionPlayCutscene
import mardek.content.action.ActionSetOverlayColor
import mardek.content.action.ActionShowChapterName
import mardek.content.action.ActionTalk
import mardek.content.action.FixedActionNode
import mardek.renderer.RenderContext
import mardek.renderer.animation.renderBattleBackgroundAnimation
import mardek.renderer.area.ui.renderCampaignDialogue
import mardek.renderer.menu.referenceTime
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.util.Rectangle
import kotlin.time.Duration

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

		val background = actions.currentBackground
		if (background != null) {
			val (animationContext, _) = createCutsceneAnimationContext(
				context, actions, region, renderTime, referenceTime,
				background.magicScale, Duration.ZERO,
			)
			renderBattleBackgroundAnimation(background.nodes, animationContext)
			animationContext.lightning.lastRenderedAt = animationContext.renderTime
		}

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

		if (action is ActionTalk) {
			val batches = renderCampaignDialogue(actions, region, context)
			colorBatch = batches.first
			simpleTextBatch = batches.second
		}

		if (action is ActionEndOfChapter && actions.endOfChapterState != null) {
			val currentChapter = context.campaign.story.evaluate(
				context.content.story.fixedVariables.chapter
			)!!
			val batches = renderEndOfChapter(context, region, currentChapter, actions.endOfChapterState!!)
			colorBatch = batches.first
			simpleTextBatch = batches.second
		}

		if (action is ActionSetOverlayColor) {
			val oldColor = actions.overlayColor
			val newColor = action.color
			val factor = (System.nanoTime() - actions.currentNodeStartTime).toFloat() /
					action.transitionTime.inWholeNanoseconds.toFloat()
			val overlayColor = interpolateColors(oldColor, newColor, factor)
			if (alpha(overlayColor) != 0.toByte()) {
				context.addColorBatch(2).fill(
					region.minX, region.minY, region.maxX, region.maxY, overlayColor
				)
			}
		} else {
			if (alpha(actions.overlayColor) != 0.toByte()) {
				context.addColorBatch(2).fill(
					region.minX, region.minY, region.maxX, region.maxY, actions.overlayColor
				)
			}
		}
	}

	return Pair(colorBatch, simpleTextBatch)
}
