package mardek.state.ingame.characters

import com.github.knokko.bitser.BitStruct
import mardek.assets.PlayableCharacter

@BitStruct(backwardCompatible = false)
// TODO Serialize the fields, when I have the tools for it
class CharactersState(
	/**
	 * The characters that the player can put in the party
	 */
	val available: MutableSet<PlayableCharacter>,

	/**
	 * The characters that the player can't put in the party, but whose items can be managed from item storage.
	 */
	val unavailable: MutableSet<PlayableCharacter>,

	/**
	 * The characters currently in the party, must all be a member of `available`
	 */
	val party: Array<PlayableCharacter?>,
) {

	internal constructor() : this(mutableSetOf(), mutableSetOf(), Array(4) { null })
}
