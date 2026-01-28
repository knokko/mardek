package mardek.state.ingame

import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaState

/**
 * The possible states of a campaign: `AreaState`, `CampaignActionsState`, or `WorldMapState`
 */
open class CampaignStateMachine {

	companion object {
		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			AreaState::class.java,
			CampaignActionsState::class.java,
		)
	}
}
