package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.sprite.KimSprite

/**
 * A `Tilesheet` is a named collection of (usually related) `Tile`s. All tiles in the game belong to a sheet.
 * In the original game, every `Area` can use only 1 sheet, but that restriction does not apply in this rewrite.
 */
@BitStruct(backwardCompatible = true)
class Tilesheet(

	/**
	 * The name of this sheet, as imported from Flash. It doesn't serve an in-game purpose, but is potentially useful
	 * for debugging and editing.
	 */
	@BitField(id = 0)
	val name: String
) {

	/**
	 * The tiles in this sheet.
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "tiles")
	val tiles = ArrayList<Tile>()

	/**
	 * The water/lava sprites of this sheet. For now, areas can only use the water sprites of their main sheet, but I
	 * intend to remove this restriction someday. It will be a list with a length of exactly 5:
	 * - Index 0 contains the water background sprite that is used when the tile to the north of the water tile is
	 *   **not** a water tile.
	 * - Index 1 contains the water background sprite that is used when the tile to the north of the water tile is
	 *   **also** a water tile.
	 * - Index 2 contains the water sprite.
	 * - Index 3 contains the lava sprite.
	 * - Index 4 contains the waterfall sprite.
	 */
	@BitField(id = 2)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 5, maxValue = 5))
	val waterSprites = ArrayList<KimSprite>()

	/**
	 * The 'empty' constructor needed by Bitser. It is also convenient for unit tests.
	 */
	constructor() : this("")
}
