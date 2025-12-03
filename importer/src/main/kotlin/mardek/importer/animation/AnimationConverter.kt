package mardek.importer.animation

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.jpexs.decompiler.flash.types.MATRIX
import mardek.content.animation.AnimationMatrix
import mardek.content.animation.ColorTransform
import kotlin.math.roundToInt

private fun transform(value: Int) = (255.0 * (value / 256.0)).roundToInt().coerceIn(0, 255)

private fun pack(r: Int, g: Int, b: Int, a: Int) = rgba(
	transform(r), transform(g), transform(b), transform(a)
)

private fun negativePack(r: Int, g: Int, b: Int, a: Int) = rgba(
	transform(-r), transform(-g), transform(-b), transform(-a)
)

internal fun convertColorTransform(color: com.jpexs.decompiler.flash.types.ColorTransform?) = if (color != null) {
	ColorTransform(
		addColor = pack(color.redAdd, color.greenAdd, color.blueAdd, color.alphaAdd),
		multiplyColor = pack(color.redMulti, color.greenMulti, color.blueMulti, color.alphaMulti),
		subtractColor = negativePack(color.redAdd, color.greenAdd, color.blueAdd, color.alphaAdd),
	)
} else null

internal fun convertTransformationMatrix(matrix: MATRIX?) = if (matrix != null) AnimationMatrix(
	translateX = matrix.translateX / 20f,
	translateY = matrix.translateY / 20f,
	rotateSkew0 = matrix.rotateSkew0,
	rotateSkew1 = matrix.rotateSkew1,
	scaleX = if (matrix.hasScale) matrix.scaleX else 1f,
	scaleY = if (matrix.hasScale) matrix.scaleY else 1f,
) else null
