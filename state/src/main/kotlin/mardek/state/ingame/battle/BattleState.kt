package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.input.InputKey

@BitStruct(backwardCompatible = false)
class BattleState(
	@BitField(ordering = 0)
	val battle: Battle,
) {
	@Suppress("unused")
	private constructor() : this(Battle())

	fun processKeyPress(key: InputKey) {

	}
}
