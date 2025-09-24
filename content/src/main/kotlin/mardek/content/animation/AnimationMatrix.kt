package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

@BitStruct(backwardCompatible = true)
class AnimationMatrix(

	// For some reason, I need to divide all flash translations by 20 (and 20 * 0.05 = 1)
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.05)
	val translateX: Float,

	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05)
	val translateY: Float,

	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val rotateSkew0: Float,

	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val rotateSkew1: Float,

	@BitField(id = 4)
	val hasScale: Boolean,

	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	private val scaleX: Float,

	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	private val scaleY: Float,
) {
	constructor() : this(0f, 0f, 0f, 0f, false, 0f, 0f)

	companion object {
		val DEFAULT = AnimationMatrix(
			0f, 0f, 0f, 0f, false, 1f, 1f
		)
	}

	fun getScaleX() = if (hasScale) scaleX else 1f

	fun getScaleY() = if (hasScale) scaleY else 1f

	override fun equals(other: Any?): Boolean {
		if (other !is AnimationMatrix) return false
		if (this.translateX != other.translateX || this.translateY != other.translateY) return false
		if (this.rotateSkew0 != other.rotateSkew0 || this.rotateSkew1 != other.rotateSkew1) return false
		return this.getScaleX() == other.getScaleX() && this.getScaleY() == other.getScaleY()
	}

	override fun hashCode() = translateX.hashCode() + 13 * translateY.hashCode() -
			31 * rotateSkew0.hashCode() - 59 * rotateSkew1.hashCode()
}
