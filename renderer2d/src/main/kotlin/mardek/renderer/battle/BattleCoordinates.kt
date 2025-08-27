package mardek.renderer.battle

import mardek.content.battle.PartyLayoutPosition

fun computeMagicScale(context: BattleRenderContext): Float {
	// Original resolution is 240x176
	val stage = context.context.currentStage
	return 0.5f * stage.height / 176f
}

fun transformBattleCoordinates(rawPosition: PartyLayoutPosition, flipX: Float, context: BattleRenderContext): TransformedCoordinates {
	val magicScale = computeMagicScale(context)

	val rawX = 0.5f * (1f + flipX) * context.context.currentStage.width - flipX * (rawPosition.x + 38) * magicScale
	val rawY = magicScale * (rawPosition.y + 100)

	return TransformedCoordinates(rawX, rawY, magicScale)
}

class TransformedCoordinates(var x: Float, var y: Float, val scale: Float) {

	override fun toString() = "(x=$x, y=$y, scale=$scale)"
}
