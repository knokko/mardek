package mardek.renderer.battle

import mardek.renderer.RenderContext
import mardek.state.ingame.CampaignState
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleUpdateContext

class BattleRenderContext(
	val context: RenderContext,
	val state: CampaignState,
	val battle: BattleState
) {
	val renderTime = System.nanoTime()

	val updateContext = BattleUpdateContext(
		state.characterStates,
		context.content.audio.fixedEffects,
		context.content.stats.elements.find { it.rawName == "NONE" }!!,
		context.state.soundQueue,
	)
}
