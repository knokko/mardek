package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.state.ingame.battle.Battle
import kotlin.time.Duration

@BitStruct(backwardCompatible = false)
class IncomingRandomBattle(

	@BitField(ordering = 0)
	val battle: Battle,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true)
	val startAt: Duration,

	@BitField(ordering = 2)
	val canAvoid: Boolean,
) {
	@Suppress("unused")
	private constructor() : this(Battle(), Duration.ZERO, false)
}