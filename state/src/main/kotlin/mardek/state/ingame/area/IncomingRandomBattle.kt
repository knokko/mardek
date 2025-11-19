package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.battle.Battle
import kotlin.time.Duration

@BitStruct(backwardCompatible = true)
class IncomingRandomBattle(

	@BitField(id = 0)
	val battle: Battle,

	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val startAt: Duration,

	@BitField(id = 2)
	val canAvoid: Boolean,
) {
	@Suppress("unused")
	private constructor() : this(Battle(), Duration.ZERO, false)
}
