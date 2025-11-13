package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.area.TransitionDestination
import mardek.content.sprite.ArrowSprite

/**
 * Represents an area transition. When the player steps on an area transition, that player is 'teleported' to another
 * area, or to the world map. Area transitions are rendered as blue arrows whose opacity/alpha oscillates.
 */
@BitStruct(backwardCompatible = true)
class AreaTransition(

	/**
	 * The X-coordinate of the tile containing the area transition
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of the tile containing the area transition
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The destination of the area transition: the player will be teleported to this destination upon stepping on the
	 * area transition
	 */
	@BitField(id = 2)
	val destination: TransitionDestination,

	/**
	 * The blue arrow that should be rendered above the tile containing this transition, or `null` if the transition
	 * should be invisible. (When it is 'invisible', it could still be placed on top of a special tile, like a cave
	 * entrance.)
	 */
	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "arrow sprites")
	val arrow: ArrowSprite?,
) {

	@Suppress("unused")
	private constructor() : this(0, 0, TransitionDestination(), null)

	override fun toString() = "Transition(x=$x, y=$y, arrow=$arrow, destination=$destination)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
