package mardek.state

import mardek.assets.Campaign
import mardek.input.InputManager
import mardek.state.title.TitleScreenState
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

class StartupState(private val campaign: CompletableFuture<Campaign>): GameState {
	override fun update(input: InputManager, timeStep: Duration, soundQueue: SoundQueue): GameState {
		if (campaign.isDone) return TitleScreenState(campaign.join())
		if (campaign.isCompletedExceptionally || campaign.isCancelled) throw RuntimeException("Failed to load campagin")
		return this
	}
}
