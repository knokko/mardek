package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.sprite.KimSprite

/**
 * A `Tilesheet` is a named collection of `Tile`s. All tiles in the game belong to a sheet. In the original game, every
 * `Area` can use only 1 sheet, but that restriction does not apply in this rewrite.
 */
@BitStruct(backwardCompatible = true)
class Tilesheet(
	@BitField(id = 0)
	val name: String
) {
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "tiles")
	val tiles = ArrayList<Tile>()

	/**
	 * The water/lava sprites of this sheet. For now, areas can only use the water sprites of their main sheet, but I
	 * intend to remove this restriction someday.
	 */
	@BitField(id = 2)
	val waterSprites = ArrayList<KimSprite>()

	/**
	 * The 'empty' constructor needed by Bitser. It is also convenient for unit tests.
	 */
	constructor() : this("")
}
