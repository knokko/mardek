package mardek.content.area

import com.github.knokko.bitser.BitEnum

/**
 * The types of water sprites that tiles can have
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class WaterType {

	/**
	 * The tile is **not** a water tile
	 */
	None,

	/**
	 * Tiles must **not** have this water type. This field exists only to give the remaining `WaterType` a more
	 * convenient `ordinal`.
	 */
	Skipped,

	/**
	 * The tile is a basic water tile
	 */
	Water,

	/**
	 * The tile is a lava tile
	 */
	Lava,

	/**
	 * The tile is a waterfall tile
	 */
	Waterfall
}
