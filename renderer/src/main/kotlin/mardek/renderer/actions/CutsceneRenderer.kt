package mardek.renderer.actions

import com.github.knokko.bitser.ReferenceLazyBits
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
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
	context: RenderContext, actions: CampaignActionsState, action: ActionPlayCutscene,
	region: Rectangle, createTextBatch: (capacity: Int) -> Vk2dFancyTextBatch,
): Vk2dColorBatch? {
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

	if (frameIndex == -1) return null

	for (textEntry in action.cutscene.payload.get().subtitles) {
		if (frameIndex >= textEntry.frame) actions.cutsceneSubtitle = Pair(textEntry.index, textEntry.text)
	}

	val (animationContext, relativeScaleX) = createCutsceneAnimationContext(
		context, actions, region, actions.currentNodeStartTime,
		action.cutscene.payload.get().magicScale, allFrames.duration,
	)
	renderCutsceneAnimation(ReferenceLazyBits(allFrames), animationContext)
	animationContext.lightning.lastRenderedAt = context.timing.now()

	if (actions.cutsceneSubtitle.second.isNotEmpty()) {
		val font = context.bundle.getFont(context.content.fonts.large2.index)
		val textHeight = 0.015f * region.width * relativeScaleX

		val batch = createTextBatch(500)
		fun draw(baseX: Float, baseY: Float, alignment: TextAlignment) {
			batch.drawShadowedString(
				actions.cutsceneSubtitle.second, baseX, baseY, 0f, textHeight, font,
				MardekTextStyles.Cutscenes.CAPTION, alignment,
			)
		}
		if (actions.cutsceneSubtitle.first == 0) {
			draw(
				region.minX + 0.01f * region.width,
				region.maxY - 0.015f * region.height,
				TextAlignment.LEFT,
			)
		}
		if (actions.cutsceneSubtitle.first == 1) {
			draw(
				region.minX + 0.5f * region.width,
				region.maxY - 0.033f * region.height,
				TextAlignment.CENTERED,
			)
		}
		if (actions.cutsceneSubtitle.first == 2) {
			draw(
				region.maxX - 0.01f * region.width,
				region.maxY - 0.033f * region.height,
				TextAlignment.RIGHT,
			)
		}
	}

	if (action.hasFadeOut) {
		val fadeAlpha = context.timing.interpolate(
			actions.currentNodeStartTime + (allFrames.duration - 1.seconds), 0,
			1.seconds, 255, true
		)
		if (fadeAlpha > 0) {
			colorBatch = context.addColorBatch(50)
			colorBatch.fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0, 0, 0, fadeAlpha),
			)
		}
	}

	return colorBatch
}
