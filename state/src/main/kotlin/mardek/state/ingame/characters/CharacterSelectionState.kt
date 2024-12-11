package mardek.state.ingame.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.characters.PlayableCharacter

@BitStruct(backwardCompatible = false)
class CharacterSelectionState(
	/**
	 * The characters that the player can put in the party
	 */
	@BitField(ordering = 0)
	@ReferenceField(stable = true, label = "playable characters")
	val available: HashSet<PlayableCharacter>,

	/**
	 * The characters that the player can't put in the party, but whose items can be managed from item storage.
	 */
	@BitField(ordering = 1)
	@ReferenceField(stable = true, label = "playable characters")
	val unavailable: HashSet<PlayableCharacter>,

	/**
	 * The characters currently in the party, must all be a member of `available`
	 */
	@BitField(ordering = 2)
	@NestedFieldSetting(path = "c", optional = true)
	@ReferenceField(stable = true, label = "playable characters")
	val party: Array<PlayableCharacter?>,
) {

	internal constructor() : this(HashSet(), HashSet(), Array(4) { null })
}
