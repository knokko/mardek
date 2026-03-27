package mardek.renderer.battle

import mardek.content.battle.PartyLayoutPosition
import mardek.state.util.Rectangle

fun transformBattleCoordinates(rawPosition: PartyLayoutPosition, flipX: Float, region: Rectangle): TransformedCoordinates {
	val rawX = 0.5f * (1f + flipX) * region.width - flipX * rawPosition.distanceX * region.height
	val rawY = rawPosition.distanceY * region.height

	return TransformedCoordinates(rawX, rawY)
}

class TransformedCoordinates(var x: Float, var y: Float) {

	override fun toString() = "(x=$x, y=$y)"
}
