package mardek.renderer.ui

import mardek.state.title.AbsoluteRectangle
import kotlin.math.floor
import kotlin.math.roundToInt

abstract class CoordinateTransform(val width: Int, val height: Int) {

	abstract fun transformX(x: Float): Int

	abstract fun transformY(y: Float): Int

	abstract fun transformWidth(width: Float): Int

	abstract fun transformHeight(height: Float): Int

	fun transform(minX: Float, minY: Float, width: Float, height: Float) = AbsoluteRectangle(
		minX = transformX(minX), minY = transformY(minY + height),
		width = transformWidth(width), height = transformHeight(height)
	)

	companion object {
		fun create(layout: SpaceLayout, width: Int, height: Int): CoordinateTransform = when (layout) {
			SpaceLayout.Simple -> SimpleCoordinateTransform(width, height)
			SpaceLayout.GrowRight -> HorizontalCoordinateTransform(width, height)
			else -> throw UnsupportedOperationException("Unsupported layout $layout")
		}
	}
}

internal class SimpleCoordinateTransform(width: Int, height: Int): CoordinateTransform(width, height) {

	override fun transformX(x: Float) = floor(x * this.width).roundToInt()

	override fun transformY(y: Float) = floor((1f - y) * this.height).roundToInt()

	override fun transformWidth(width: Float) = (width * this.width).roundToInt()

	override fun transformHeight(height: Float) = (height * this.height).roundToInt()
}

internal class HorizontalCoordinateTransform(width: Int, height: Int): CoordinateTransform(width, height) {

	override fun transformX(x: Float) = floor(x * this.height).roundToInt()

	override fun transformY(y: Float) = floor((1f - y) * this.height).roundToInt()

	override fun transformWidth(width: Float) = (width * this.height).roundToInt()

	override fun transformHeight(height: Float) = (height * this.height).roundToInt()
}
