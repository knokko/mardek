package mardek.renderer.battle

import mardek.renderer.InGameRenderContext
import mardek.state.ingame.battle.BattleState

class BattleRenderContext(
	val battle: BattleState,
	inGameContext: InGameRenderContext,
): InGameRenderContext(inGameContext.content, inGameContext.campaign, inGameContext)
