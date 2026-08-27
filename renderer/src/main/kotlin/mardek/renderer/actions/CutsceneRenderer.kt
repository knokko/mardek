package mardek.renderer.actions

import com.github.knokko.bitser.ReferenceLazyBits
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.action.ActionPlayCutscene
import mardek.content.util.Time
import mardek.renderer.MardekTextStyles
import mardek.renderer.RenderContext
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.LightningInfo
import mardek.renderer.animation.renderCutsceneAnimation
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal fun createCutsceneAnimationContext(
	context: RenderContext, actions: CampaignActionsState, region: Rectangle,
	referenceTime: Time, magicScale: Int, duration: Duration,
): Pair<AnimationContext, Float> {
	val partBatch = context.addAnimationPartBatch(50)
	val inverseScaleX = 450f
	val inverseScaleY = 270f
	val magicRenderScaleX = region.width / inverseScaleX
	val magicRenderScaleY = region.height / inverseScaleY
	val magicRenderScale = max(magicRenderScaleX, magicRenderScaleY)

	val renderWidth = inverseScaleX * magicRenderScale
	val renderHeight = inverseScaleY * magicRenderScale
	val clippedWidth = max(0f, renderWidth - region.width)
	val clippedHeight = max(0f, renderHeight - region.height)

	if (actions.lightningRenderInfo !is LightningInfo) {
		actions.lightningRenderInfo = LightningInfo()
	}

	val animationContext = AnimationContext(
		renderRegion = region,
		referenceTime = referenceTime,
		timing = context.timing,
		magicScale = magicScale,
		parentMatrix = Matrix3x2f().translate(
			region.minX + 0.5f * (renderWidth - clippedWidth),
			region.minY + 0.5f * (renderHeight - clippedHeight),
		).scale(magicRenderScale),
		parentColorTransform = null,
		partBatch = partBatch,
		noMask = context.content.battle.noMask,
		combat = null,
		portrait = null,
		currentChapter = context.campaign.story.evaluate(context.content.story.fixedVariables.chapter) ?: 0,
		lightning = actions.lightningRenderInfo as LightningInfo,
		animationDuration = duration,
	)

	return Pair(animationContext, magicRenderScaleX / magicRenderScale)
}

internal fun renderCutscene(
	context: RenderContext, actions: CampaignActionsState, action: ActionPlayCutscene, fullRegion: Rectangle,
	createSimpleTextBatch: (capacity: Int) -> Vk2dSimpleTextBatch,
	createFancyTextBatch: (capacity: Int) -> Vk2dFancyTextBatch,
): Pair<Vk2dColorBatch?, Vk2dSimpleTextBatch?> {
	var colorBatch: Vk2dColorBatch? = null

	val allFrames = action.cutscene.payload.get().frames
	val timeSinceStart = context.timing.elapsedTimeSince(actions.currentNodeStartTime)
	var remainingTime = timeSinceStart
	var frameIndex = -1
	for ((currentFrameIndex, frame) in allFrames.withIndex()) {
		remainingTime -= frame.duration
		if (remainingTime < Duration.ZERO) {
			frameIndex = currentFrameIndex
			break
		}
	}

	val showSkipButton = allFrames.duration > 5.seconds
	val skipBarHeight = if (showSkipButton) fullRegion.height / 10 else 0
	val contentRegion = Rectangle(
		fullRegion.minX, fullRegion.minY + skipBarHeight,
		fullRegion.width, fullRegion.height - skipBarHeight
	)

	if (frameIndex == -1) return Pair(null, null)
	var simpleTextBatch: Vk2dSimpleTextBatch? = null

	for (textEntry in action.cutscene.payload.get().subtitles) {
		if (frameIndex >= textEntry.frame) {
			actions.cutsceneRendering.subtitle = Pair(textEntry.index, textEntry.text)
		}
	}

	val (animationContext, relativeScaleX) = createCutsceneAnimationContext(
		context, actions, contentRegion, actions.currentNodeStartTime,
		action.cutscene.payload.get().magicScale, allFrames.duration,
	)
	renderCutsceneAnimation(ReferenceLazyBits(allFrames), animationContext)
	animationContext.lightning.lastRenderedAt = context.timing.now()

	if (showSkipButton) {
		val font = context.bundle.getFont(context.content.fonts.basic2.index)
		simpleTextBatch = createSimpleTextBatch(100)
		simpleTextBatch.drawString(
			"(Hold E or Q to speed up)",
			fullRegion.minX + 0.02f * fullRegion.width, fullRegion.minY + 0.06f * fullRegion.height,
			0.02f * fullRegion.height, font,
			MardekTextStyles.Cutscenes.SKIP_HINT, TextAlignment.LEFT,
		)

		simpleTextBatch.drawString(
			"SKIP", fullRegion.maxX - 0.08f * fullRegion.height, fullRegion.minY + 0.065f * fullRegion.height,
			0.03f * fullRegion.height, font,
			MardekTextStyles.Cutscenes.SKIP_LABEL, TextAlignment.RIGHT
		)

		val arrow = context.content.ui.arrowHead
		val arrowMinX = (fullRegion.maxX - 0.06f * fullRegion.height).roundToInt()
		val arrowMinY = (fullRegion.minY + 0.025f * fullRegion.height).roundToInt()
		val (addColor, multiplyColor) = if (actions.cutsceneRendering.isOnSkipButton(actions.mouseX, actions.mouseY)) Pair(
			rgba(0f, 0f, 0.3f, 0f),
			rgba(0.0f, 0.1f, 0.5f, 1f),
		) else Pair(0, -1)
		context.addImageBatch(2).coloredScale(
			arrowMinX.toFloat(), arrowMinY.toFloat(), 0.05f * fullRegion.height / arrow.height,
			arrow.index, addColor, multiplyColor,
		)
		val arrowHeight = fullRegion.height / 20
		val arrowWidth = arrow.width * arrowHeight / arrow.height
		actions.cutsceneRendering.skipButton = Rectangle(arrowMinX, arrowMinY, arrowWidth, arrowHeight)
	}

	if (actions.cutsceneRendering.subtitle.second.isNotEmpty()) {

		val font = context.bundle.getFont(context.content.fonts.large2.index)
		val textHeight = 0.015f * contentRegion.width * relativeScaleX

		val batch = createFancyTextBatch(500)
		fun draw(baseX: Float, baseY: Float, alignment: TextAlignment) {
			batch.drawShadowedString(
				actions.cutsceneRendering.subtitle.second, baseX, baseY, 0f, textHeight, font,
				MardekTextStyles.Cutscenes.CAPTION, alignment,
			)
		}
		if (actions.cutsceneRendering.subtitle.first == 0) {
			draw(
				contentRegion.minX + 0.01f * contentRegion.width,
				contentRegion.maxY - 0.015f * contentRegion.height,
				TextAlignment.LEFT,
			)
		}
		if (actions.cutsceneRendering.subtitle.first == 1) {
			draw(
				contentRegion.minX + 0.5f * contentRegion.width,
				contentRegion.maxY - 0.033f * contentRegion.height,
				TextAlignment.CENTERED,
			)
		}
		if (actions.cutsceneRendering.subtitle.first == 2) {
			draw(
				contentRegion.maxX - 0.01f * contentRegion.width,
				contentRegion.maxY - 0.033f * contentRegion.height,
				TextAlignment.RIGHT,
			)
		}
	}

	if (action.hasFadeOut) {
		val fadeAlpha = context.timing.interpolate(
			actions.currentNodeStartTime.virtualAdd(allFrames.duration - 1.seconds), 0,
			1.seconds, 255, true
		)
		if (fadeAlpha > 0) {
			colorBatch = context.addColorBatch(50)
			colorBatch.fill(
				fullRegion.minX, fullRegion.minY, fullRegion.maxX, fullRegion.maxY,
				rgba(0, 0, 0, fadeAlpha),
			)
		}
	}

	return Pair(colorBatch, simpleTextBatch)
}
