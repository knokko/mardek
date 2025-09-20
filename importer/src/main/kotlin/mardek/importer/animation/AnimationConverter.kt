package mardek.importer.animation

import com.github.knokko.boiler.utilities.ColorPacker.rgba
import com.jpexs.decompiler.flash.types.MATRIX
import mardek.content.animation.AnimationMatrix
import mardek.content.animation.ColorTransform
import kotlin.math.roundToInt

private fun transform(value: Int) = (255.0 * (value / 256.0)).roundToInt()
private fun pack(r: Int, g: Int, b: Int, a: Int) = rgba(
	transform(r), transform(g), transform(b), transform(a)
)

internal fun convertColorTransform(color: com.jpexs.decompiler.flash.types.ColorTransform?) = if (color != null) {
	ColorTransform(
		addColor = pack(color.redAdd, color.greenAdd, color.blueAdd, color.alphaAdd),
		multiplyColor = pack(color.redMulti, color.greenMulti, color.blueMulti, color.alphaMulti)
	)
} else null

internal fun convertTransformationMatrix(matrix: MATRIX?) = if (matrix != null) AnimationMatrix(
	translateX = matrix.translateX / 20f,
	translateY = matrix.translateY / 20f,
	rotateSkew0 = matrix.rotateSkew0,
	rotateSkew1 = matrix.rotateSkew1,
	hasScale = matrix.hasScale,
	scaleX = matrix.scaleX,
	scaleY = matrix.scaleY
) else null
