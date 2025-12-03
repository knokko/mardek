package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import mardek.content.sprite.KimSprite

/**
 * Represents a tile that can appear in the tile grid of an `Area`. Note that the same `Tile` can appear multiple times
 * in the same area, and can appear in multiple areas.
 */
@BitStruct(backwardCompatible = true)
class Tile(

	/**
	 * The sprites of the tile. Most tiles have just 1 sprite, but higher tiles (e.g. trees) have multiple sprites.
	 * When a tile at `(x, y)` has multiple sprites:
	 * - the last sprite will be drawn at `(x, y)`
	 * - the second-last sprite will be drawn at `(x, y - 1)`
	 * - the first sprite will be drawn at `(x, y + 1 - sprites.length)`
	 */
	@BitField(id = 0)
	@NestedFieldSetting(path = "", sizeField = IntegerField(minValue = 1, maxValue = 3, expectUniform = true))
	val sprites: ArrayList<KimSprite>,

	/**
	 * True if and only if players can walk on this tile, assuming that nothing (e.g. a non-player character) blocks
	 * the player.
	 */
	@BitField(id = 1)
	val canWalkOn: Boolean,

	/**
	 * This field determines whether there will be water above this tile, and if so, what kind of water.
	 */
	@BitField(id = 2)
	val waterType: WaterType
) {

	@Suppress("unused")
	private constructor() : this(ArrayList(0), false, WaterType.None)
}
