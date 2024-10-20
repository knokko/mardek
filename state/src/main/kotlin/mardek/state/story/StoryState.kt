package mardek.state.story

import mardek.state.character.PlayableCharacter

class StoryState(val heroMardek: PlayableCharacter, val heroDeugan: PlayableCharacter) {

	val party = arrayOf(heroMardek, heroDeugan, null, null)

	fun getPlayableCharacters() = setOf(heroMardek, heroDeugan)
}
