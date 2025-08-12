package mardek.renderer.battle

import mardek.content.battle.PartyLayoutPosition
import mardek.renderer.RenderContext
import kotlin.math.min
import kotlin.math.roundToInt

fun computeMagicScale(context: BattleRenderContext): Pair<Float, Float> {
	// Original resolution is 240x176
	var magicScaleX = 1f / 240f
	val magicScaleY2 = 1f / 176f

	val frame = context.context.frame
	val magicScaleY1 = min(magicScaleY2, magicScaleX * frame.width / frame.height)
	magicScaleX = min(magicScaleX, magicScaleY1 * frame.height / frame.width)
	return Pair(magicScaleX, magicScaleY2)
}

fun transformBattleCoordinates(rawPosition: PartyLayoutPosition, flipX: Float, context: BattleRenderContext): TransformedCoordinates {
	val (magicScaleX, magicScaleY) = computeMagicScale(context)

	val rawX = -flipX * (-1f + (rawPosition.x + 38) * magicScaleX)
	val rawRelativeY = rawPosition.y + 78 - 176
	val rawY = rawRelativeY * magicScaleY

	return TransformedCoordinates(rawX, rawY, magicScaleX, magicScaleY)
}

class TransformedCoordinates(var x: Float, var y: Float, val scaleX: Float, val scaleY: Float) {

	override fun toString() = "(x=$x, y=$y, scaleX=$scaleX, scaleY=$scaleY)"

	fun intX(width: Int) = intX(x, width)

	fun intY(height: Int) = intY(y, height)

	companion object {
		fun intX(floatX: Float, width: Int) = ((0.5f + 0.5f * floatX) * width).roundToInt()

		fun intY(floatY: Float, height: Int) = ((0.5f + 0.5f * floatY) * height).roundToInt()
	}
}
