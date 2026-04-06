package mardek.state.ingame.battle

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.Element
import mardek.state.SoundQueue
import mardek.content.characters.CharacterState
import mardek.state.ingame.encyclopedia.EncyclopediaState

class BattleUpdateContext(
	val characterStates: Map<PlayableCharacter, CharacterState>,

	/**
	 * The encyclopedia state, from [mardek.state.ingame.CampaignState.encyclopedia]
	 */
	val encyclopedia: EncyclopediaState,
	val sounds: FixedSoundEffects,
	val physicalElement: Element,
	val soundQueue: SoundQueue,
) {
	internal constructor() : this(
		emptyMap(), EncyclopediaState(),
		FixedSoundEffects(), Element(), SoundQueue(),
	)
}
