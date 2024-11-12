package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.input.InputManager
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.characters.CharactersState
import kotlin.time.Duration

@BitStruct(backwardCompatible = false)
class GameProgression(

	@BitField(ordering = 0, optional = true)
	var currentArea: AreaState?,

	@BitField(ordering = 1)
	val characters: CharactersState,
) {

	@Suppress("unused")
	private constructor() : this(null, CharactersState())

	fun update(input: InputManager, timeStep: Duration) {
		currentArea?.update(input, timeStep)
	}
}
