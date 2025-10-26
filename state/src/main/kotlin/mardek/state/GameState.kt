package mardek.state

import mardek.content.Content
import mardek.input.InputManager
import mardek.state.saves.SavesFolderManager
import kotlin.time.Duration

interface GameState {
	fun updateBeforeContent(input: InputManager, soundQueue: SoundQueue, saves: SavesFolderManager) = this

	fun update(context: GameStateUpdateContext): GameState
}

open class GameStateUpdateContext(
	val content: Content,
	val input: InputManager,
	val soundQueue: SoundQueue,
	val timeStep: Duration,
	val saves: SavesFolderManager = SavesFolderManager(),
) {
	constructor(copy: GameStateUpdateContext) : this(
		copy.content, copy.input,
		copy.soundQueue, copy.timeStep,
		copy.saves,
	)
}
