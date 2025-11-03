package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

/**
 * Represents a color-transformation (in an animation). When an `AnimationSprite` is rendered, the color of each pixel
 * will be transformed by a `ColorTransform` before being rendered. (At least, if at least 1 ancestor has a
 * `ColorTransform`.)
 *
 * The final pixel color will be `addColor + multiplyColor * originalPixelColor`. The `ColorPacker` class of vk-boiler
 * is used to squeeze `addColor` and `multiplyColor` into 32-bit integers.
 */
@BitStruct(backwardCompatible = true)
class ColorTransform(

	/**
	 * The color that will be added to the final color
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true)
	val addColor: Int,

	/**
	 * The color with which the original color will be multiplied
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val multiplyColor: Int,
) {

	@Suppress("unused")
	private constructor() : this(0, 0)

	private fun formatComponent(color: Int, offset: Int) = String.format("%.1f", ((color shr offset) and 255) / 255f)

	private fun formatColor(color: Int) = "(${formatComponent(color, 0)}, " +
			"${formatComponent(color, 8)}, ${formatComponent(color, 16)}, " +
			"${formatComponent(color, 24)})"

	override fun toString() = "ColorTransform(add ${formatColor(addColor)}, multiply ${formatColor(multiplyColor)})"

	override fun equals(other: Any?) = other is ColorTransform && addColor == other.addColor &&
			multiplyColor == other.multiplyColor

	override fun hashCode() = addColor + 13 * multiplyColor

	companion object {
		/**
		 * The default color transform that does nothing: it adds rgba(0, 0, 0, 0) and multiplies with (-1, -1, -1, -1)
		 */
		val DEFAULT = ColorTransform(0, -1)
	}
}
