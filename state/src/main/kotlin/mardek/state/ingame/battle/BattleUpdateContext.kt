package mardek.state.ingame.battle

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.Element
import mardek.state.SoundQueue
import mardek.state.ingame.characters.CharacterState

class BattleUpdateContext(
	val characterStates: Map<PlayableCharacter, CharacterState>,
	val sounds: FixedSoundEffects,
	val physicalElement: Element,
	val soundQueue: SoundQueue,
) {
	internal constructor() : this(emptyMap(), FixedSoundEffects(), Element(), SoundQueue())
}
