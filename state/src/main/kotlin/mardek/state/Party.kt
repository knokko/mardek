package mardek.state

import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter

/**
 * Represents a non-empty party member slot
 */
data class UsedPartyMember(
	/**
	 * The index of the party member slot, which should be an integer between 0 and 3
	 */
	val index: Int,

	/**
	 * The playable character in the party member slot
	 */
	val character: PlayableCharacter,

	/**
	 * The current state of `character`
	 */
	val state: CharacterState,
)

typealias WholeParty = Array<Pair<PlayableCharacter, CharacterState>?>
