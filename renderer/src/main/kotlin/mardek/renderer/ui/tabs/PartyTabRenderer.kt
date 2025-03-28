package mardek.renderer.ui.tabs

/////////////////////////////////////////////////////
// importing renderer resources (?)
import mardek.renderer.SharedResources
import mardek.renderer.batch.KimBatch
import mardek.renderer.batch.KimRequest
/////////////////////////////////////////////////////

//////////////////////////////////////////////////////
// Importing actual game state
import mardek.state.ingame.characters.CharacterState
import mardek.state.ingame.CampaignState
//////////////////////////////////////////////////////

import mardek.content.Content

import mardek.state.title.AbsoluteRectangle

import mardek.content.characters.PlayableCharacter

///////////////
// Constants //
///////////////
private const val PARTY_BAR_HEIGHT = 30;
private const val BAR_MARGIN = 5;

class PartyTabRenderer(
	private val state: CampaignState,
	private val content: Content,
	private val resources: SharedResources,
): TabRenderer() {

	override fun beforeRendering()
	{
		// nothing to do before rendering yet
	}

	override fun render()
	{

	}
}