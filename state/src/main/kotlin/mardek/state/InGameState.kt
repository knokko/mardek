package mardek.state

import mardek.input.InputManager
import mardek.state.area.AreaState
import mardek.state.story.StoryState
import kotlin.time.Duration

class InGameState(val area: AreaState, val story: StoryState): GameState {
	override fun update(input: InputManager, timeStep: Duration): GameState {
		area.update(input, timeStep)
		return this
	}
}
