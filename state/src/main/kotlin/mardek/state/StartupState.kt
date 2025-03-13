package mardek.state

import mardek.content.Content
import mardek.input.InputManager
import mardek.state.title.TitleScreenState
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

class StartupState(private val content: CompletableFuture<Content>): GameState {
	override fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue): GameState {
		if (content.isDone) return TitleScreenState(content.join())
		if (content.isCompletedExceptionally || content.isCancelled) throw RuntimeException("Failed to load campagin")
		return this
	}
}
