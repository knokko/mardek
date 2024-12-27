package mardek.importer.characters

import mardek.assets.characters.PlayableCharacter
import mardek.assets.inventory.Item
import mardek.assets.skill.Skill
import mardek.assets.inventory.ItemStack

class FatPlayableCharacter(
	val wrapped: PlayableCharacter,
	val initialLevel: Int,
	val initialEquipment: List<Item?>,
	val initialItems: List<ItemStack>,
	val initialMasteredSkills: List<Skill>,
	val initialToggledSkills: Set<Skill>,
) {
	override fun toString() = wrapped.toString()
}
