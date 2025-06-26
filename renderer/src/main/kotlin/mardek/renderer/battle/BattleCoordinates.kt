package mardek.renderer.battle

import com.github.knokko.boiler.images.VkbImage
import mardek.content.battle.PartyLayoutPosition
import kotlin.math.min
import kotlin.math.roundToInt

fun computeMagicScale(targetImage: VkbImage): Pair<Float, Float> {
	// Original resolution is 240x176
	var magicScaleX = 1f / 240f
	val magicScaleY2 = 1f / 176f
	val magicScaleY1 = min(magicScaleY2, magicScaleX * targetImage.width / targetImage.height)
	magicScaleX = min(magicScaleX, magicScaleY1 * targetImage.height / targetImage.width)
	return Pair(magicScaleX, magicScaleY2)
}

fun transformBattleCoordinates(rawPosition: PartyLayoutPosition, flipX: Float, targetImage: VkbImage): TransformedCoordinates {
	val (magicScaleX, magicScaleY) = computeMagicScale(targetImage)

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
