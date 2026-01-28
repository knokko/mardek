package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.area.AreaTransitionDestination
import mardek.content.area.TransitionDestination
import mardek.content.sprite.ObjectSprites

/**
 * Represents a portal or dream circle in an area. Players will be moved to the `destination` when stepping on the
 * portal.
 *
 * A portal is considered to be a dream circle if and only if the transition destination is an area
 * (and not e.g. the world map), and when exactly 1 of the current area and destination area is a dreamworld area.
 * Otherwise, it is just a portal, with the texture/animation of just a portal.
 */
@BitStruct(backwardCompatible = true)
class AreaPortal(

	x: Int,
	y: Int,

	/**
	 * The destination of this portal: the player will be teleported to the destination upon stepping on the portal.
	 */
	@BitField(id = 0)
	@ClassField(root = TransitionDestination::class)
	val destination: TransitionDestination
) : StaticAreaObject(x, y) {

	/**
	 * The sprites/animation of this portal, or `null` if the portal is invisible. This field is ignored for dream
	 * circles.
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "object sprites")
	var sprites: ObjectSprites? = null

	@Suppress("unused")
	private constructor() : this(0, 0, AreaTransitionDestination())

	override fun toString() = "Portal(x=$x, y=$y, destination=$destination)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
