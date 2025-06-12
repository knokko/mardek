package mardek.state

import mardek.content.Content
import mardek.input.InputManager
import kotlin.time.Duration

interface GameState {
	fun update(context: GameStateUpdateContext): GameState
}

open class GameStateUpdateContext(
	val content: Content,
	val input: InputManager,
	val soundQueue: SoundQueue,
	val timeStep: Duration
) {
	constructor(copy: GameStateUpdateContext) : this(
		copy.content, copy.input,
		copy.soundQueue, copy.timeStep
	)
}
