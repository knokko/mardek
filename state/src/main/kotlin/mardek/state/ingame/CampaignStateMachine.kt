package mardek.state.ingame

import mardek.input.Event
import mardek.state.ingame.actions.CampaignActionsState
import mardek.state.ingame.area.AreaState
import mardek.state.ingame.worldmap.WorldMapState

/**
 * The possible states of a campaign: `AreaState`, `CampaignActionsState`, or `WorldMapState`
 */
abstract class CampaignStateMachine {

	/**
	 * This method should be invoked whenever the campaign receives an event, while this is the state of the campaign
	 */
	abstract fun processEvent(event: Event, campaignContext: CampaignState.UpdateContext, campaign: CampaignState)

	/**
	 * This method should be invoked during every [CampaignState.update], while this is the state of the campaign
	 */
	abstract fun update(campaignContext: CampaignState.UpdateContext, campaign: CampaignState)

	companion object {

		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			AreaState::class.java,
			CampaignActionsState::class.java,
			WorldMapState::class.java,
		)
	}
}
