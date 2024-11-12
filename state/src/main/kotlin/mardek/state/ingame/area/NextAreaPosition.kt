package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import kotlin.time.Duration

@BitStruct(backwardCompatible = false)
class NextAreaPosition(

	@BitField(ordering = 0)
	val position: AreaPosition,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true)
	val startTime: Duration,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = true)
	val arrivalTime: Duration,
) {

	override fun toString() = "($position at $arrivalTime)"
}
