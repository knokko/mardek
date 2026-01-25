package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import mardek.content.area.Area
import kotlin.math.max
import kotlin.math.min

/**
 * Tracks which tiles of a single area have already been *discovered* on the area map.
 *
 * When tiles are *discovered*, players can see them when they open the area map in their in-game menu. Players
 * automatically discover tiles that are close to their main character (normally Mardek). By walking through the area,
 * more and more tiles will be discovered automatically.
 *
 * Note that this class tracks the discovery of a single area, whereas [AreaDiscoveryMap] tracks the discovery of all
 * areas (using 1 `AreaDiscovery` per area).
 */
@BitStruct(backwardCompatible = true)
class AreaDiscovery(area: Area) {

	@BitField(id = 0)
	@IntegerField(expectUniform = false)
	private var minTileX = area.minTileX

	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	private var minTileY = area.minTileY

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	private var width = area.width

	@BitField(id = 3)
	@NestedFieldSetting(path = "", writeAsBytes = true)
	private var raw = BooleanArray(width * (1 + area.height))

	@Suppress("unused")
	private constructor() : this(Area())

	/**
	 * This method must be called whenever a save is loaded. It will check whether the size and offset of the area are
	 * the same as when the save was created.
	 *
	 * If the size and offset are the same (which is true almost all the time), this method does nothing.
	 *
	 * But, when the size and/or offset have changed, this method will shift and resize the raw discovery data
	 * (which is just a boolean array). This is to ensure that e.g. `isDiscovered(4, 8)` yields the same result
	 * after loading the save.
	 */
	internal fun validateOffsetsAndSize(area: Area) {
		val oldHeight = raw.size / width - 1
		if (minTileX == area.minTileX && minTileY == area.minTileY && width == area.width && oldHeight == area.height) {
			// When the area size and/or offsets haven't changed, we don't need to shift/resize the discovery data
			return
		}

		val maxTileX = minTileX + width - 1
		val maxTileY = minTileY + oldHeight - 1
		val newRaw = BooleanArray(area.width * (1 + area.height))
		for (y in max(area.minTileY, minTileY)  .. 1 + min(area.maxTileY, maxTileY)) {
			for (x in max(area.minTileX, minTileX) .. min(area.maxTileX, maxTileX)) {
				if (isDiscovered(x, y)) newRaw[(x - area.minTileX) + (y - area.minTileY) * area.width] = true
			}
		}

		this.minTileX = area.minTileX
		this.minTileY = area.minTileY
		this.width = area.width
		this.raw = newRaw
	}

	/**
	 * Checks whether the tile at `(x, y)` is discovered. When `x` or `y` are out of bounds, an exception may or may
	 * not be thrown.
	 */
	fun isDiscovered(x: Int, y: Int) = raw[(x - minTileX) + (y - minTileY) * width]

	/**
	 * Discovers the tiles around `(playerX, playerY)`. This method should be called whenever the player moves to a
	 * new tile.
	 */
	fun discover(playerX: Int, playerY: Int) {
		val height = raw.size / width
		for (x in max(minTileX, playerX - RADIUS) .. min(minTileX + width - 1, playerX + RADIUS)) {
			for (y in max(minTileY, playerY - RADIUS) .. min(minTileY + height - 1, playerY + RADIUS)) {
				raw[(x - minTileX) + (y - minTileY) * width] = true
			}
		}
	}

	companion object {

		/**
		 * The area discovery radius.
		 *
		 * When a player moves to `(x, y)`, the tiles between `(x - RADIUS, y - RADIUS)` and `(x + RADIUS, y + RADIUS)`
		 * will be discovered (both inclusive).
		 */
		internal const val RADIUS = 9
	}
}
