package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.sprite.BcSprite

@BitStruct(backwardCompatible = true)
class AnimationSprite(
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = -1, maxValue = 8000)
	val defineShapeFlashID: Int,

	@BitField(id = 2)
	val image: BcSprite,

	@BitField(id = 3)
	@FloatField(expectMultipleOf = 0.05)
	val offsetX: Float,

	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.05)
	val offsetY: Float,
) {
	internal constructor() : this(-1, BcSprite(), 0f, 0f)

	override fun toString() = "AnimationSprite(flash shape ID = $defineShapeFlashID)"
}
