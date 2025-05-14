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
		inGameContext.campaign.characterStates, inGameContext.content.audio.fixedEffects, SoundQueue()
	)
}
