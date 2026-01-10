package mardek.state.ingame.menu

import mardek.content.audio.FixedSoundEffects
import mardek.content.inventory.ItemStack
import mardek.content.skill.SkillsContent
import mardek.state.SoundQueue
import mardek.state.UsedPartyMember
import mardek.state.WholeParty

class UiUpdateContext(
	val usedParty: List<UsedPartyMember>,
	val fullParty: WholeParty,
	val soundQueue: SoundQueue,
	val sounds: FixedSoundEffects,
	val skills: SkillsContent,
	val getCursorStack: () -> ItemStack?,
	val setCursorStack: (ItemStack?) -> Unit,
)
