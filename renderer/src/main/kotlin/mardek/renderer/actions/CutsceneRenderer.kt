package mardek.renderer.actions

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear
import com.github.knokko.vk2d.text.TextAlignment
import mardek.content.action.ActionPlayCutscene
import mardek.renderer.RenderContext
import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.renderCutsceneAnimation
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.math.max

internal fun renderCutscene(
	context: RenderContext, actions: CampaignActionsState, action: ActionPlayCutscene,
	renderTime: Long, region: Rectangle, createTextBatch: (capacity: Int) -> MardekGlyphBatch,
) {
	val allFrames = action.cutscene.payload.get().frames
	var remainingTime = renderTime - actions.currentNodeStartTime
	var frameIndex = -1
	for ((currentFrameIndex, frame) in allFrames.withIndex()) {
		remainingTime -= frame.duration.inWholeNanoseconds
		if (remainingTime < 0L) {
			frameIndex = currentFrameIndex
			break
		}
	}

	if (frameIndex == -1) {
		actions.finishedAnimationNode = true
	} else {
		for (textEntry in action.cutscene.payload.get().subtitles) {
			if (frameIndex >= textEntry.frame) actions.cutsceneSubtitle = Pair(textEntry.index, textEntry.text)
		}

		val partBatch = context.addAnimationPartBatch(25)
		val inverseScaleX = 450f
		val inverseScaleY = 270f
		val magicScaleX = region.width / inverseScaleX
		val magicScaleY = region.height / inverseScaleY
		val magicScale = max(magicScaleX, magicScaleY)

		val renderWidth = inverseScaleX * magicScale
		val renderHeight = inverseScaleY * magicScale
		val clippedWidth = max(0f, renderWidth - region.width)
		val clippedHeight = max(0f, renderHeight - region.height)
		val animationContext = AnimationContext(
			renderRegion = region,
			renderTime = renderTime,
			referenceTime = actions.currentNodeStartTime,
			magicScale = action.cutscene.payload.get().magicScale,
			parentMatrix = Matrix3x2f().translate(
				region.minX + 0.5f * (renderWidth - clippedWidth),
				region.minY + 0.5f * (renderHeight - clippedHeight),
			).scale(magicScale),
			parentColorTransform = null,
			partBatch = partBatch,
			noMask = context.content.battle.noMask,
			combat = null,
			portrait = null,
		)
		renderCutsceneAnimation(allFrames, animationContext)

		if (actions.cutsceneSubtitle.second.isNotEmpty()) {
			val font = context.bundle.getFont(context.content.fonts.large2.index)
			val outerColor = srgbToLinear(rgb(157, 230, 252))
			val innerColor = srgbToLinear(rgb(248, 255, 255))
			val shadowColor = rgb(0, 0, 255)
			val textHeight = 0.015f * region.width * magicScaleX / magicScale
			val shadowOffset = 0.08f * textHeight

			val batch = createTextBatch(300)
			fun draw(baseX: Float, baseY: Float, alignment: TextAlignment) {
				batch.drawFancyShadowedString(
					actions.cutsceneSubtitle.second, baseX, baseY, textHeight, font,
					outerColor, 0, 0f,
					outerColor, innerColor, innerColor, outerColor,
					0.15f, 0.15f, 0.55f, 0.55f, shadowColor,
					shadowOffset, shadowOffset, alignment
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

		val timeUntilFinish = actions.currentNodeStartTime + allFrames.duration.inWholeNanoseconds - renderTime
		val fadeTime = 1_000_000_000L
		if (timeUntilFinish < 1_000_000_000L) {
			val fade = 1f - timeUntilFinish.toFloat() / fadeTime
			context.addColorBatch(2).fill(
				region.minX, region.minY, region.maxX, region.maxY,
				rgba(0f, 0f, 0f, fade),
			)
		}
	}
}
