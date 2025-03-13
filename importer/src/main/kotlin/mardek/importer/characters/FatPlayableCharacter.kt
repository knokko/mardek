package mardek.importer.characters

import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item
import mardek.content.skill.Skill
import mardek.content.inventory.ItemStack

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
