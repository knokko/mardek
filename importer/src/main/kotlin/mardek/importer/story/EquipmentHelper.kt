package mardek.importer.story

import mardek.content.characters.CharacterState
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item

fun forceEquipment(owner: PlayableCharacter, ownerState: CharacterState, index: Int, item: Item) {
	val slot = owner.characterClass.equipmentSlots[index]
	if (!slot.isAllowed(item, owner)) throw IllegalArgumentException("Slot $slot rejects $item")
	ownerState.equipment[slot] = item
}
