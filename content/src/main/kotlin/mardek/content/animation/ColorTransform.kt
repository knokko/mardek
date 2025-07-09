package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class ColorTransform(

	@BitField(id = 0)
	@IntegerField(expectUniform = true)
	val addColor: Int,

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
		val DEFAULT = ColorTransform(0, -1)
	}
}
