package mardek.state

import com.github.knokko.bitser.serialize.Bitser
import mardek.content.Content
import mardek.input.InputManager
import mardek.state.title.AbsoluteRectangle
import kotlin.time.Duration

class GameStateManager(private val input: InputManager, var currentState: GameState) {

	@Volatile
	var crossLocation: AbsoluteRectangle? = null

	@Volatile
	var maximizeLocation: AbsoluteRectangle? = null

	@Volatile
	var minusLocation: AbsoluteRectangle? = null

	@Volatile
	var hoveringCross = false

	@Volatile
	var hoveringMaximize = false

	@Volatile
	var hoveringMinus = false

	val soundQueue = SoundQueue()

	fun lock(): Any = this

	fun update(content: Content, timeStep: Duration) {
		this.currentState = this.currentState.update(GameStateUpdateContext(content, input, soundQueue, timeStep))
	}

	companion object {
		val bitser = Bitser(true)
	}
}
