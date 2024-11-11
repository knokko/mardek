package mardek.state.story

import mardek.assets.PlayableCharacter

class StoryState(val heroMardek: PlayableCharacter, val heroDeugan: PlayableCharacter) {

	val party = arrayOf(heroMardek, heroDeugan, null, null)

	fun getPlayableCharacters() = setOf(heroMardek, heroDeugan)
}
