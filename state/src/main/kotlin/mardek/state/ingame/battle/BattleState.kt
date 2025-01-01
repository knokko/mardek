package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.characters.PlayableCharacter
import mardek.input.InputKey

@BitStruct(backwardCompatible = false)
class BattleState(
	@BitField(ordering = 0)
	val battle: Battle,

	@BitField(ordering = 1)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceField(stable = true, label = "playable characters")
	val players: Array<PlayableCharacter?>,
) {

	val startTime = System.nanoTime()

	@Suppress("unused")
	private constructor() : this(Battle(), emptyArray())

	fun processKeyPress(key: InputKey) {

	}
}
