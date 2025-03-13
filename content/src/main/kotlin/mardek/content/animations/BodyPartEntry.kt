package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.sprite.BcSprite

@BitStruct(backwardCompatible = true)
class BodyPartEntry(
	@BitField(id = 0)
	val sprite: BcSprite,

	// For some reason, I need to divide all flash offsets by 20 (and 20 * 0.05 = 1)
	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05, errorTolerance = 0.01)
	val offsetX: Float,

	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.05, errorTolerance = 0.01)
	val offsetY: Float,

	@BitField(id = 3)
	@IntegerField(expectUniform = false)
	val scale: Int,
) {

	@Suppress("unused")
	constructor() : this(BcSprite(), 0f, 0f, 0)
}
