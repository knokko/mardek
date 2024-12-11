package mardek.importer.characters

import mardek.assets.characters.PlayableCharacter
import mardek.assets.inventory.Item
import mardek.assets.skill.Skill
import mardek.state.ingame.inventory.ItemStack

class FatPlayableCharacter(
	val wrapped: PlayableCharacter,
	val initialEquipment: List<Item?>,
	val initialItems: List<ItemStack>,
	val initialMasteredSkills: List<Skill>,
) {
}
