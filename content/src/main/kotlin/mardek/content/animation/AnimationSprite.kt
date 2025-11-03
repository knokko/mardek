package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.sprite.BcSprite

/**
 * Represents a sprite (leaf) of an animation node/tree.
 */
@BitStruct(backwardCompatible = true)
class AnimationSprite(

	/**
	 * When imported from flash, this is the ID of the DefineShape tag from which this `AnimationSprite` was imported
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = -1, maxValue = 8000)
	val defineShapeFlashID: Int,

	/**
	 * The image/sprite to be rendered
	 */
	@BitField(id = 2)
	val image: BcSprite,

	/**
	 * The offset on the X-axis that should be applied *after* all transformation/animation matrices
	 */
	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.05)
	val offsetX: Float,

	/**
	 * The offset on the Y-axis that should be applied *after* all transformation/animation matrices
	 */
	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.05)
	val offsetY: Float,
) {
	internal constructor() : this(-1, BcSprite(), 0f, 0f)

	override fun toString() = "AnimationSprite(flash shape ID = $defineShapeFlashID)"
}
