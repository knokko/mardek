package mardek.renderer.battle

import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.animation.renderBattleBackgroundAnimation
import org.joml.Matrix3x2f
import kotlin.math.max

internal fun renderBattleBackground(battleContext: BattleRenderContext, batch: AnimationPartBatch) {
	val background = battleContext.battle.battle.background

	val magicScaleX = batch.width / 400f
	val magicScaleY = batch.height / 260f
	val magicScale = max(magicScaleX, magicScaleY)

	val animationContext = AnimationContext(
		renderTime = battleContext.renderTime,
		magicScale = background.magicScale,
		parentMatrix = Matrix3x2f().scale(magicScale),
		parentColorTransform = null,
		partBatch = batch,
		noMask = battleContext.context.content.battle.noMask,
		combat = null,
	)
	renderBattleBackgroundAnimation(background.nodes, animationContext)
}
