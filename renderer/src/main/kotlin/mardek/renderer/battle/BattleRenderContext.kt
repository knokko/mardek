package mardek.renderer.battle

import mardek.renderer.InGameRenderContext
import mardek.state.SoundQueue
import mardek.state.ingame.battle.BattleState
import mardek.state.ingame.battle.BattleUpdateContext

class BattleRenderContext(
	val battle: BattleState,
	inGameContext: InGameRenderContext,
): InGameRenderContext(inGameContext.campaign, inGameContext) {

	val updateContext = BattleUpdateContext(
		campaign.characterStates, content.audio.fixedEffects,
		content.stats.elements.find { it.rawName == "NONE" }!!, SoundQueue()
	)
}
