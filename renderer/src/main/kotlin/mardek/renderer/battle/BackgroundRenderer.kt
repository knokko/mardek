package mardek.renderer.battle

import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.animation.renderBattleBackgroundAnimation
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import kotlin.math.max

internal fun renderBattleBackground(battleContext: BattleRenderContext, batch: AnimationPartBatch, region: Rectangle) {
	val background = battleContext.battle.battle.background

	val magicScaleX = region.width / 400f
	val magicScaleY = region.height / 260f
	val magicScale = max(magicScaleX, magicScaleY)

	val renderWidth = 400f * magicScale
	val renderHeight = 260f * magicScale
	val clippedWidth = max(0f, renderWidth - region.width)
	val clippedHeight = max(0f, renderHeight - region.height)
	val animationContext = AnimationContext(
		renderRegion = region,
		renderTime = battleContext.renderTime,
		magicScale = background.magicScale,
		parentMatrix = Matrix3x2f().translate(
			region.minX - clippedWidth * 0.5f, region.minY - clippedHeight * 0.25f
		).scale(magicScale),
		parentColorTransform = null,
		partBatch = batch,
		noMask = battleContext.context.content.battle.noMask,
		combat = null,
		portrait = null,
	)
	renderBattleBackgroundAnimation(background.nodes, animationContext)
}
