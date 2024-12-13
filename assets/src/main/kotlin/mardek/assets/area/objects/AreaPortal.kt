package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.area.TransitionDestination
import mardek.assets.sprite.ObjectSprites

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
	@ReferenceField(stable = false, label = "object sprites")
	var sprites: ObjectSprites? = null

	@Suppress("unused")
	private constructor() : this(0, 0, TransitionDestination())

	override fun toString() = "Portal(x=$x, y=$y, destination=$destination)"

	override fun equals(other: Any?) = other is AreaPortal && x == other.x && y == other.y &&
			destination == other.destination && sprites == other.sprites

	override fun hashCode() = destination.hashCode()
}
