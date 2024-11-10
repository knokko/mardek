package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.TransitionDestination
import mardek.assets.area.sprites.ObjectSpritesheet

@BitStruct(backwardCompatible = false)
class AreaTransition(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 2)
	val destination: TransitionDestination,

	@BitField(ordering = 3, optional = true)
	val arrow: String?
) {

	@BitField(ordering = 4, optional = true)
	var arrowSprite: ObjectSpritesheet? = null

	@Suppress("unused")
	private constructor() : this(0, 0, TransitionDestination(), null)

	override fun toString() = "Transition(x=$x, y=$y, arrow=$arrow, destination=$destination)"

	override fun equals(other: Any?) = other is AreaTransition && x == other.x && y == other.y &&
			destination == other.destination && arrow == other.arrow

	override fun hashCode() = x - 13 * y + 17 * destination.hashCode()
}
