package mardek.renderer.battle

import com.github.knokko.boiler.images.VkbImage
import mardek.content.battle.PartyLayoutPosition
import kotlin.math.min
import kotlin.math.roundToInt

fun transformBattleCoordinates(rawPosition: PartyLayoutPosition, flipX: Float, targetImage: VkbImage): TransformedCoordinates {
	// Original resolution is 240x176
	var magicScaleX = 1f / 240f
	val magicScaleY2 = 1f / 176f
	val magicScaleY1 = min(magicScaleY2, magicScaleX * targetImage.width / targetImage.height)
	magicScaleX = min(magicScaleX, magicScaleY1 * targetImage.height / targetImage.width)

	val rawX = -flipX * (-1f + (rawPosition.x + 38) * magicScaleX)
	val rawRelativeY = rawPosition.y + 78 - 176
	val rawY = rawRelativeY * magicScaleY2

	return TransformedCoordinates(rawX, rawY, magicScaleX, magicScaleY1)
}

class TransformedCoordinates(val x: Float, val y: Float, val scaleX: Float, val scaleY: Float) {

	override fun toString() = "(x=$x, y=$y, scaleX=$scaleX, scaleY=$scaleY)"

	fun intX(width: Int) = ((0.5f + 0.5f * x) * width).roundToInt()

	fun intY(height: Int) = ((0.5f + 0.5f * y) * height).roundToInt()
}
