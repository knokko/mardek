package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.sprite.KimSprite

/**
 * Represents a pair of sprites for a chest: it contains a base/closed chest sprite and an opened chest sprite.
 */
@BitStruct(backwardCompatible = true)
class ChestSprite(

	/**
	 * The flash ID of the chest sprite pair. I don't think it is used outside of importing.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 5)
	val flashID: Int,

	/**
	 * The base/closed sprite of the chest
	 */
	@BitField(id = 1)
	val baseSprite: KimSprite,

	/**
	 * The opened sprite of the chest
	 */
	@BitField(id = 2)
	val openedSprite: KimSprite,
) {

	constructor() : this(0, KimSprite(), KimSprite())
}
