package mardek.renderer.battle

import mardek.content.battle.PartyLayoutPosition
import mardek.state.util.Rectangle

fun computeMagicScale(region: Rectangle): Float {
	// This magic scale appears to be the best, don't question it...
	return 0.0032f * region.height
}

fun transformBattleCoordinates(rawPosition: PartyLayoutPosition, flipX: Float, region: Rectangle): TransformedCoordinates {
	val magicScale = computeMagicScale(region)

	val rawX = 0.5f * (1f + flipX) * region.width - flipX * (rawPosition.x + 60) * magicScale
	// TODO CHAP1 Figure out how this stuff works, this is almost certainly wrong...
	val rawY = magicScale * (rawPosition.y + if (flipX > 0f) 122 else 127)

	return TransformedCoordinates(rawX, rawY, magicScale)
}

class TransformedCoordinates(var x: Float, var y: Float, val scale: Float) {

	override fun toString() = "(x=$x, y=$y, scale=$scale)"
}
