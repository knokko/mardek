package mardek.state.ingame.battle

import mardek.content.audio.FixedSoundEffects
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.Element
import mardek.state.SoundQueue
import mardek.content.characters.CharacterState
import mardek.state.ingame.CampaignStatistics
import mardek.state.ingame.encyclopedia.EncyclopediaState

/**
 * This class is used as the 'parameter type' of several methods of [BattleState]. Using this class avoids the need to
 * repeat all parameters to every method invocation. It also makes it easier to add parameters, since they don't need
 * to be explicitly propagated between all methods.
 */
class BattleUpdateContext(

	/**
	 * The [mardek.state.ingame.CampaignState.characterStates]
	 */
	val characterStates: Map<PlayableCharacter, CharacterState>,

	/**
	 * The encyclopedia state, from [mardek.state.ingame.CampaignState.encyclopedia]
	 */
	val encyclopedia: EncyclopediaState,

	/**
	 * The campaign statistics, from [mardek.state.ingame.CampaignState.statistics]
	 */
	val statistics: CampaignStatistics,

	/**
	 * The [mardek.content.audio.AudioContent.fixedEffects]
	 */
	val sounds: FixedSoundEffects,

	/**
	 * The `NONE`/`PHYSICAL` [Element]
	 */
	val physicalElement: Element,

	/**
	 * The queue where all sounds-to-be-played should be inserted
	 */
	val soundQueue: SoundQueue,
) {
	internal constructor() : this(
		emptyMap(), EncyclopediaState(), CampaignStatistics(),
		FixedSoundEffects(), Element(), SoundQueue(),
	)
}
