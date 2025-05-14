package mardek.state.ingame.battle

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.PlayableCharacter
import mardek.state.SoundQueue
import mardek.state.ingame.characters.CharacterState

class BattleUpdateContext(
	val characterStates: Map<PlayableCharacter, CharacterState>,
	val sounds: FixedSoundEffects,
	val soundQueue: SoundQueue,
)
