package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER
import mardek.content.area.Direction

/**
 * The state of an `AreaCharacter`. This contains their position, their rotation, and a potential next position (when
 * the character is currently walking)
 */
@BitStruct(backwardCompatible = true)
class AreaCharacterState(

	/**
	 * The X-coordinate of the tile on which this character is standing
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false)
	val x: Int,

	/**
	 * The Y-coordinate of the tile on which this character is standing
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val y: Int,

	/**
	 * The direction in which this character is looking
	 */
	@BitField(id = 2)
	val direction: Direction,

	/**
	 * - When this character is standing still, `next` will be null.
	 * - When this character is walking, `next` is the tile/position to which this character is walking. This must be a
	 *   neighbouring tile.
	 */
	@BitField(id = 3, optional = true)
	val next: NextAreaPosition?,
) {

	@Suppress("unused")
	private constructor() : this(0, 0, Direction.Down, null)

	override fun hashCode() = BITSER.hashCode(this)

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun toString() = "($x, $y, $direction, next=$next)"

	/**
	 * Convenience method for `AreaPosition(x, y)`
	 */
	fun toPosition() = AreaPosition(x, y)
}
