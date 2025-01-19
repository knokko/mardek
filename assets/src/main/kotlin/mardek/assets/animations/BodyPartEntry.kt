package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.sprite.BcSprite

@BitStruct(backwardCompatible = false)
class BodyPartEntry(
	@BitField(ordering = 0)
	val sprite: BcSprite,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false)
	val offsetX: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false)
	val offsetY: Int,
) {

	constructor() : this(BcSprite(), 0, 0)
}
