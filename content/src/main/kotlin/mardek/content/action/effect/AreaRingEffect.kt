package mardek.content.action.effect

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * A sub-effect that can be emitted by [AreaEffectsEmitter]s.
 * These effects can be used to generate special effects in the shape of a circle or ring.
 *
 * This effect will render a ring between the [innerBorder] and [outerBorder] (or a circle when the radius of the
 * inner border is 0). The colors of the ring will be linearly interpolated between [innerBorder] and [outerBorder].
 */
@BitStruct(backwardCompatible = true)
class AreaRingEffect(

	/**
	 * The radius and color of the 'inner border' of the ring
	 */
	@BitField(id = 0)
	val innerBorder: Border,

	/**
	 * The radius and color of the 'outer border' of the ring
	 */
	@BitField(id = 1)
	val outerBorder: Border,
) {

	@Suppress("unused")
	private constructor() : this(Border(), Border())

	/**
	 * Represents the radius and color of either the [innerBorder] or [outerBorder].
	 */
	@BitStruct(backwardCompatible = true)
	class Border(

		/**
		 * The radius function
		 */
		@BitField(id = 0)
		val radius: AreaEffectFloat,

		/**
		 * The function for the red (R) component of the color
		 */
		@BitField(id = 1)
		val red: AreaEffectFloat,

		/**
		 * The function for the green (G) component of the color
		 */
		@BitField(id = 2)
		val green: AreaEffectFloat,

		/**
		 * The function for the blue (B) component of the color
		 */
		@BitField(id = 3)
		val blue: AreaEffectFloat,

		/**
		 * The function for the alpha (A) component of the color
		 */
		@BitField(id = 4)
		val alpha: AreaEffectFloat,
	) {

		internal constructor() : this(
			AreaEffectFloat(), AreaEffectFloat(), AreaEffectFloat(),
			AreaEffectFloat(), AreaEffectFloat(),
		)
	}
}
