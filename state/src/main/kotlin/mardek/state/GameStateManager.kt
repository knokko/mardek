package mardek.state

import com.github.knokko.bitser.serialize.Bitser
import mardek.content.Content
import mardek.input.InputManager
import mardek.state.saves.SavesFolderManager
import mardek.state.util.Rectangle
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

class GameStateManager(
	private val input: InputManager,
	var currentState: GameState,
	private val saves: SavesFolderManager,
) {

	@Volatile
	var crossLocation: Rectangle? = null

	@Volatile
	var maximizeLocation: Rectangle? = null

	@Volatile
	var minusLocation: Rectangle? = null

	@Volatile
	var hoveringCross = false

	@Volatile
	var hoveringMaximize = false

	@Volatile
	var hoveringMinus = false

	val soundQueue = SoundQueue()

	fun lock(): Any = this

	fun update(content: CompletableFuture<Content>, timeStep: Duration) {
		if (content.isDone) {
			val context = GameStateUpdateContext(content.get(), input, soundQueue, timeStep, saves)
			this.currentState = this.currentState.update(context)
		} else {
			this.currentState = this.currentState.updateBeforeContent(input, soundQueue, saves)
		}
	}

	companion object {
		val bitser = Bitser(true)
	}
}
