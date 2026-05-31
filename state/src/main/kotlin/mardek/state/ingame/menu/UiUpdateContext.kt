package mardek.state.ingame.menu

import mardek.content.audio.FixedSoundEffects
import mardek.content.inventory.ItemStack
import mardek.content.skill.SkillsContent
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignStatistics
import mardek.state.ingame.UsedPartyMember
import mardek.state.ingame.WholeParty

/**
 * This class is used as parameter type for many update-related methods related to the state of the in-game menu.
 * By putting all parameters of such methods in this class, they can all be propagated by passing around 1 instance of
 * this class.
 */
class UiUpdateContext(

	/**
	 * The result of [mardek.state.ingame.CampaignState.usedPartyMembers]
	 */
	val usedParty: List<UsedPartyMember>,

	/**
	 * The result of [mardek.state.ingame.CampaignState.allPartyMembers]
	 */
	val fullParty: WholeParty,

	/**
	 * The [SoundQueue] where sounds-to-be-played should be inserted
	 */
	val soundQueue: SoundQueue,

	/**
	 * The [mardek.content.audio.AudioContent.fixedEffects]
	 */
	val sounds: FixedSoundEffects,
	val skills: SkillsContent,

	/**
	 * The [mardek.state.ingame.CampaignState.statistics]
	 */
	val statistics: CampaignStatistics,

	/**
	 * Gets [mardek.state.ingame.CampaignState.cursorItemStack]
	 */
	val getCursorStack: () -> ItemStack?,

	/**
	 * Sets [mardek.state.ingame.CampaignState.cursorItemStack]
	 */
	val setCursorStack: (ItemStack?) -> Unit,
)
