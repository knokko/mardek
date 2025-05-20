package mardek.state.ingame.menu

import mardek.content.Content

import mardek.state.ingame.CampaignState

import mardek.input.InputKey

import mardek.state.SoundQueue

class PartyTab(private val state: CampaignState): InGameMenuTab(false) 
{
	var partyIndex = state.characterSelection.party.indexOfFirst { it != null }
	// val for costants
	val squaresCount = 7
	override fun getText() = "Party"

	// var for mutable properties
	var activeValue = 0

	override fun processKeyPress(key: InputKey, soundQueue: SoundQueue)
	{
		if (key == InputKey.MoveLeft)
		{
			if (activeValue > 0)
				activeValue -= 1
			soundQueue.insert("menu-party-scroll")
		}

		if (key == InputKey.MoveRight)
		{
			if (activeValue < 6)
				activeValue += 1
			soundQueue.insert("menu-party-scroll")
		}
		super.processKeyPress(key, soundQueue)
	}

}
