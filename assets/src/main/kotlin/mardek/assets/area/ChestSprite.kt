package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.sprite.KimSprite

@BitStruct(backwardCompatible = false)
class ChestSprite(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 5)
	val flashID: Int,

	@BitField(ordering = 1)
	val baseSprite: KimSprite,

	@BitField(ordering = 2)
	val openedSprite: KimSprite,
) {

	constructor() : this(0, KimSprite(), KimSprite())
}
