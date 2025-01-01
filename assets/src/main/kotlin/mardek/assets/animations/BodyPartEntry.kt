package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.sprite.BcSprite

@BitStruct(backwardCompatible = false)
class BodyPartEntry(
	@BitField(ordering = 0)
	val sprite: BcSprite,

	// For some reason, I need to divide all flash offsets by 20 (and 20 * 0.05 = 1)
	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.05, errorTolerance = 0.01)
	val offsetX: Float,

	@BitField(ordering = 2)
	@FloatField(expectMultipleOf = 0.05, errorTolerance = 0.01)
	val offsetY: Float,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false)
	val scale: Int,
) {

	@Suppress("unused")
	constructor() : this(BcSprite(), 0f, 0f, 0)
}
