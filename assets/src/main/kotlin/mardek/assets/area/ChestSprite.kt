package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class ChestSprite(
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 5)
	val flashID: Int,

	@BitField(id = 1)
	val baseSprite: KimSprite,

	@BitField(id = 2)
	val openedSprite: KimSprite,
) {

	constructor() : this(0, KimSprite(), KimSprite())
}
