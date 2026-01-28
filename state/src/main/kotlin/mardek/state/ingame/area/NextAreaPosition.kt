package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.area.TransitionDestination
import kotlin.time.Duration

@BitStruct(backwardCompatible = true)
class NextAreaPosition(

	@BitField(id = 0)
	val position: AreaPosition,

	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val startTime: Duration,

	@BitField(id = 2)
	@IntegerField(expectUniform = true)
	val arrivalTime: Duration,

	@BitField(id = 3, optional = true)
	@ClassField(root = TransitionDestination::class)
	val transition: TransitionDestination?,
) {

	internal constructor() : this(AreaPosition(), Duration.ZERO, Duration.ZERO, null)

	override fun toString() = "($position at $arrivalTime)"
}
