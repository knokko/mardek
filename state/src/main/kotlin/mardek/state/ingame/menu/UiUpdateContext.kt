package mardek.state.ingame.menu

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.PlayableCharacter
import mardek.content.skill.SkillsContent
import mardek.state.SoundQueue
import mardek.state.ingame.characters.CharacterSelectionState
import mardek.state.ingame.characters.CharacterState

class UiUpdateContext(
	val characterSelection: CharacterSelectionState,
	val characterStates: Map<PlayableCharacter, CharacterState>,
	val soundQueue: SoundQueue,
	val sounds: FixedSoundEffects,
	val skills: SkillsContent
)
