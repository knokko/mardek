package mardek.state.ingame.menu

import mardek.content.Content

import mardek.state.ingame.CampaignState

class PartyTab(private val state: CampaignState): InGameMenuTab(false) {
	var partyIndex = state.characterSelection.party.indexOfFirst { it != null }
	override fun getText() = "Party"
}
