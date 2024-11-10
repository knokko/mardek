package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.area.TransitionDestination
import mardek.assets.area.sprites.ObjectSpritesheet

@BitStruct(backwardCompatible = false)
class AreaPortal(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(ordering = 2)
	val destination: TransitionDestination
) {

	@BitField(ordering = 3, optional = true)
	var spritesheet: ObjectSpritesheet? = null

	@Suppress("unused")
	private constructor() : this(0, 0, TransitionDestination())

	override fun toString() = "Portal(x=$x, y=$y, destination=$destination)"

	override fun equals(other: Any?) = other is AreaPortal && x == other.x && y == other.y &&
			destination == other.destination

	override fun hashCode() = destination.hashCode()
}
