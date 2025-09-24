package mardek.renderer.battle

import mardek.renderer.animation.AnimationContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.animation.renderPortraitAnimation
import mardek.state.ingame.battle.BattleMoveSelectionAttack
import mardek.state.ingame.battle.BattleMoveSelectionItem
import mardek.state.ingame.battle.BattleMoveSelectionSkill
import mardek.state.ingame.battle.BattleStateMachine
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f

internal fun renderBattlePortrait(battleContext: BattleRenderContext, batch: AnimationPartBatch, fullRegion: Rectangle) {
	battleContext.run {
		val stateMachine = battle.state
		if (stateMachine !is BattleStateMachine.SelectMove) return

		val move = stateMachine.selectedMove
		if (move is BattleMoveSelectionAttack && move.target != null) return
		if (move is BattleMoveSelectionSkill && move.target != null) return
		if (move is BattleMoveSelectionItem && move.target != null) return

		val magicScale = fullRegion.height / 400f

		val renderWidth = 50f * magicScale
		val renderHeight = 65f * magicScale

		val renderX = fullRegion.boundX - renderWidth
		val renderY = fullRegion.boundY - renderHeight - fullRegion.height / 8 - fullRegion.height / 12

		val portraitInfo = stateMachine.onTurn.player.portraitInfo
		val animationContext = AnimationContext(
			renderTime = battleContext.renderTime,
			magicScale = context.content.portraits.magicScale,
			parentMatrix = Matrix3x2f().translate(renderX, renderY).scale(magicScale),
			parentColorTransform = null,
			partBatch = batch,
			noMask = battleContext.context.content.battle.noMask,
			combat = null,
			portrait = portraitInfo,
			portraitExpression = "norm",
		)
		renderPortraitAnimation(context.content.portraits.animations, animationContext)
	}
}
