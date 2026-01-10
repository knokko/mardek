package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.action.ActionSequence
import mardek.content.area.objects.AreaObjects
import java.util.*

/**
 * Represents an *area*. An area is basically a grid of tiles with possibly some chests, non-player characters, and
 * other objects. Almost the entire game takes place in an area.
 */
@BitStruct(backwardCompatible = true)
class Area(

	/**
	 * The width of the area: the number of columns in the tile grid
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,

	/**
	 * The height of the area: the number of rows in the tile grid
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val height: Int,

	/**
	 * The primary tilesheet of the area. The tilesheet currently determines the water sprites of the area.
	 */
	@BitField(id = 2)
	@ReferenceField(stable = false, label = "tilesheets")
	val tilesheet: Tilesheet,

	/**
	 * The tile grid of the area:
	 * - The length of this array must be `width * height`
	 * - The tile at (x, y) is stored at index `x + y * width`. Please use the `getTile(x, y)` method to do this.
	 */
	@BitField(id = 3)
	@ReferenceField(stable = false, label = "tiles")
	val tileGrid: Array<Tile>,

	/**
	 * The *objects* in this area: non-player characters, doors, portals, etc...
	 */
	@BitField(id = 4)
	val objects: AreaObjects,

	/**
	 * The chests in this area
	 */
	@BitField(id = 5)
	@ReferenceFieldTarget(label = "chests")
	val chests: ArrayList<Chest>,

	/**
	 * This field determines whether the area has random battles. When an area has random battles, the game will
	 * automatically transition to a battle when the player keeps walking in that area. This field will be non-null if
	 * and only if the area has random battles.
	 */
	@BitField(id = 6, optional = true)
	val randomBattles: RandomAreaBattles?,

	/**
	 * Some flags that each area may or may not have, for instance whether warping is allowed.
	 */
	@BitField(id = 7)
	val flags: AreaFlags,

	/**
	 * The properties that areas can have, for instance their name and music track
	 */
	@BitField(id = 8)
	val properties: AreaProperties,

	/**
	 * The unique ID of the area, which is used for (de)serialization
	 */
	@BitField(id = 9)
	@StableReferenceFieldId
	val id: UUID,
) {

	/**
	 * The action sequences that can happen in this area. Each action sequence can be referenced by e.g. walk triggers
	 * or non-player characters.
	 */
	@BitField(id = 10)
	@ReferenceFieldTarget(label = "action sequences")
	val actions = ArrayList<ActionSequence>()

	constructor() : this(
		0, 0, Tilesheet(), emptyArray(), AreaObjects(), ArrayList(),
		null, AreaFlags(), AreaProperties(), UUID.randomUUID(),
	)

	override fun toString() = properties.displayName

	/**
	 * Checks whether players can walk on the tile at the given coordinates. Note that this method only takes *static*
	 * conditions into account (e.g. whether the tile itself is accessible), but no *dynamic* conditions (e.g. whether
	 * this tile is occupied by a non-player character). The `AreaState` class has the rest of the logic for this.
	 */
	fun canWalkOnTile(x: Int, y: Int): Boolean {
		if (x !in 0 until width || y !in 0 until height) return false
		if (!getTile(x, y).canWalkOn) return false

		for (chest in chests) {
			if (x == chest.x && y == chest.y) return false
		}

		for (door in objects.doors) {
			if (x == door.x && y == door.y) return false
		}

		for (orb in objects.switchOrbs) {
			if (x == orb.x && y == orb.y) return false
		}

		return true
	}

	/**
	 * Gets the tile at the given coordinates. When the coordinates are out-of-bounds, an exception may or may not be
	 * thrown.
	 */
	fun getTile(x: Int, y: Int) = tileGrid[x + y * width]
}
