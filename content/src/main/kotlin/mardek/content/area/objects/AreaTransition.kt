package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.area.TransitionDestination
import mardek.content.sprite.ArrowSprite

@BitStruct(backwardCompatible = true)
class AreaTransition(

	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(id = 2)
	val destination: TransitionDestination,

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
